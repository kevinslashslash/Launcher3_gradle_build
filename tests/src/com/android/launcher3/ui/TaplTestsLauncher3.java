/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.ui;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.popup.ArrowPopup;
import com.android.launcher3.tapl.AllApps;
import com.android.launcher3.tapl.AppIcon;
import com.android.launcher3.tapl.AppIconMenu;
import com.android.launcher3.tapl.AppIconMenuItem;
import com.android.launcher3.tapl.Folder;
import com.android.launcher3.tapl.FolderIcon;
import com.android.launcher3.tapl.Widgets;
import com.android.launcher3.tapl.Workspace;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import com.android.launcher3.widget.picker.WidgetsRecyclerView;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TaplTestsLauncher3 extends AbstractLauncherUiTest {
    private static final String APP_NAME = "LauncherTestApp";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        initialize(this);
    }

    public static void initialize(AbstractLauncherUiTest test) throws Exception {
        test.clearLauncherData();
        test.mDevice.pressHome();
        test.waitForLauncherCondition("Launcher didn't start", launcher -> launcher != null);
        test.waitForState("Launcher internal state didn't switch to Home",
                () -> LauncherState.NORMAL);
        test.waitForResumed("Launcher internal state is still Background");
        // Check that we switched to home.
        test.mLauncher.getWorkspace();
        AbstractLauncherUiTest.checkDetectedLeaks(test.mLauncher);
    }

    // Please don't add negative test cases for methods that fail only after a long wait.
    public static void expectFail(String message, Runnable action) {
        boolean failed = false;
        try {
            action.run();
        } catch (AssertionError e) {
            failed = true;
        }
        assertTrue(message, failed);
    }

    public static boolean isWorkspaceScrollable(Launcher launcher) {
        return launcher.getWorkspace().getPageCount() > launcher.getWorkspace().getPanelCount();
    }

    private int getCurrentWorkspacePage(Launcher launcher) {
        return launcher.getWorkspace().getCurrentPage();
    }

    private WidgetsRecyclerView getWidgetsView(Launcher launcher) {
        return WidgetsFullSheet.getWidgetsView(launcher);
    }

    @Test
    public void testDevicePressMenu() throws Exception {
        mDevice.pressMenu();
        mDevice.waitForIdle();
        executeOnLauncher(
                launcher -> assertNotNull("Launcher internal state didn't switch to Showing Menu",
                        launcher.getOptionsPopup()));
        // Check that pressHome works when the menu is shown.
        mLauncher.pressHome();
    }

    @Test
    public void testPressHomeOnAllAppsContextMenu() throws Exception {
        final AllApps allApps = mLauncher.getWorkspace().switchToAllApps();
        allApps.freeze();
        try {
            allApps.getAppIcon("TestActivity7").openMenu();
        } finally {
            allApps.unfreeze();
        }
        mLauncher.pressHome();
    }

    public static void runAllAppsTest(AbstractLauncherUiTest test, AllApps allApps) {
        allApps.freeze();
        try {
            assertNotNull("allApps parameter is null", allApps);

            assertTrue(
                    "Launcher internal state is not All Apps",
                    test.isInState(() -> LauncherState.ALL_APPS));

            // Test flinging forward and backward.
            test.executeOnLauncher(launcher -> assertEquals(
                    "All Apps started in already scrolled state", 0,
                    test.getAllAppsScroll(launcher)));

            allApps.flingForward();
            assertTrue("Launcher internal state is not All Apps",
                    test.isInState(() -> LauncherState.ALL_APPS));
            final Integer flingForwardY = test.getFromLauncher(
                    launcher -> test.getAllAppsScroll(launcher));
            test.executeOnLauncher(
                    launcher -> assertTrue("flingForward() didn't scroll App Apps",
                            flingForwardY > 0));

            allApps.flingBackward();
            assertTrue(
                    "Launcher internal state is not All Apps",
                    test.isInState(() -> LauncherState.ALL_APPS));
            final Integer flingBackwardY = test.getFromLauncher(
                    launcher -> test.getAllAppsScroll(launcher));
            test.executeOnLauncher(launcher -> assertTrue("flingBackward() didn't scroll App Apps",
                    flingBackwardY < flingForwardY));

            // Test scrolling down to YouTube.
            assertNotNull("All apps: can't fine YouTube", allApps.getAppIcon("YouTube"));
            // Test scrolling up to Camera.
            assertNotNull("All apps: can't fine Camera", allApps.getAppIcon("Camera"));
            // Test failing to find a non-existing app.
            final AllApps allAppsFinal = allApps;
            expectFail("All apps: could find a non-existing app",
                    () -> allAppsFinal.getAppIcon("NO APP"));

            assertTrue(
                    "Launcher internal state is not All Apps",
                    test.isInState(() -> LauncherState.ALL_APPS));
        } finally {
            allApps.unfreeze();
        }
    }

    @Test
    @PortraitLandscape
    public void testWorkspaceSwitchToAllApps() {
        assertNotNull("switchToAllApps() returned null",
                mLauncher.getWorkspace().switchToAllApps());
        assertTrue("Launcher internal state is not All Apps",
                isInState(() -> LauncherState.ALL_APPS));
    }

    @Test
    public void testWorkspace() throws Exception {
        final Workspace workspace = mLauncher.getWorkspace();

        // Test that ensureWorkspaceIsScrollable adds a page by dragging an icon there.
        executeOnLauncher(launcher -> assertFalse("Initial workspace state is scrollable",
                isWorkspaceScrollable(launcher)));
        assertNull("Chrome app was found on empty workspace",
                workspace.tryGetWorkspaceAppIcon("Chrome"));

        workspace.ensureWorkspaceIsScrollable();

        executeOnLauncher(
                launcher -> assertEquals(
                        "Ensuring workspace scrollable didn't switch to next screen",
                        workspace.pagesPerScreen(), getCurrentWorkspacePage(launcher)));
        executeOnLauncher(
                launcher -> assertTrue("ensureScrollable didn't make workspace scrollable",
                        isWorkspaceScrollable(launcher)));
        assertNotNull("ensureScrollable didn't add Chrome app",
                workspace.getWorkspaceAppIcon("Chrome"));

        // Test flinging workspace.
        workspace.flingBackward();
        assertTrue("Launcher internal state is not Home", isInState(() -> LauncherState.NORMAL));
        executeOnLauncher(
                launcher -> assertEquals("Flinging back didn't switch workspace to page #0",
                        0, getCurrentWorkspacePage(launcher)));

        workspace.flingForward();
        executeOnLauncher(
                launcher -> assertEquals("Flinging forward didn't switch workspace to next screen",
                        workspace.pagesPerScreen(), getCurrentWorkspacePage(launcher)));
        assertTrue("Launcher internal state is not Home", isInState(() -> LauncherState.NORMAL));

        // Test starting a workspace app.
        final AppIcon app = workspace.getWorkspaceAppIcon("Chrome");
        assertNotNull("No Chrome app in workspace", app);
    }

    public static void runIconLaunchFromAllAppsTest(AbstractLauncherUiTest test, AllApps allApps) {
        allApps.freeze();
        try {
            final AppIcon app = allApps.getAppIcon("TestActivity7");
            assertNotNull("AppIcon.launch returned null", app.launch(getAppPackageName()));
            test.executeOnLauncher(launcher -> assertTrue(
                    "Launcher activity is the top activity; expecting another activity to be the "
                            + "top "
                            + "one",
                    test.isInBackground(launcher)));
        } finally {
            allApps.unfreeze();
        }
    }

    @Test
    @PortraitLandscape
    public void testAppIconLaunchFromAllAppsFromHome() throws Exception {
        final AllApps allApps = mLauncher.getWorkspace().switchToAllApps();
        assertTrue("Launcher internal state is not All Apps",
                isInState(() -> LauncherState.ALL_APPS));

        runIconLaunchFromAllAppsTest(this, allApps);
    }

    @Test
    @PortraitLandscape
    public void testWidgets() throws Exception {
        // Test opening widgets.
        executeOnLauncher(launcher ->
                assertTrue("Widgets is initially opened", getWidgetsView(launcher) == null));
        Widgets widgets = mLauncher.getWorkspace().openAllWidgets();
        assertNotNull("openAllWidgets() returned null", widgets);
        widgets = mLauncher.getAllWidgets();
        assertNotNull("getAllWidgets() returned null", widgets);
        executeOnLauncher(launcher ->
                assertTrue("Widgets is not shown", getWidgetsView(launcher).isShown()));
        executeOnLauncher(launcher -> assertEquals("Widgets is scrolled upon opening",
                0, getWidgetsScroll(launcher)));

        // Test flinging widgets.
        widgets.flingForward();
        Integer flingForwardY = getFromLauncher(launcher -> getWidgetsScroll(launcher));
        executeOnLauncher(launcher -> assertTrue("Flinging forward didn't scroll widgets",
                flingForwardY > 0));

        widgets.flingBackward();
        executeOnLauncher(launcher -> assertTrue("Flinging backward didn't scroll widgets",
                getWidgetsScroll(launcher) < flingForwardY));

        mLauncher.pressHome();
        waitForLauncherCondition("Widgets were not closed",
                launcher -> getWidgetsView(launcher) == null);
    }

    private int getWidgetsScroll(Launcher launcher) {
        return getWidgetsView(launcher).getCurrentScrollY();
    }

    private boolean isOptionsPopupVisible(Launcher launcher) {
        final ArrowPopup<?> popup = launcher.getOptionsPopup();
        return popup != null && popup.isShown();
    }

    @Test
    @PortraitLandscape
    public void testLaunchMenuItem() throws Exception {
        final AllApps allApps = mLauncher.
                getWorkspace().
                switchToAllApps();
        allApps.freeze();
        try {
            final AppIconMenu menu = allApps.
                    getAppIcon(APP_NAME).
                    openDeepShortcutMenu();

            executeOnLauncher(
                    launcher -> assertTrue("Launcher internal state didn't switch to Showing Menu",
                            isOptionsPopupVisible(launcher)));

            final AppIconMenuItem menuItem = menu.getMenuItem(1);
            assertEquals("Wrong menu item", "Shortcut 2", menuItem.getText());
            menuItem.launch(getAppPackageName());
        } finally {
            allApps.unfreeze();
        }
    }

    @Test
    @PortraitLandscape
    public void testDragAppIcon() throws Throwable {
        // 1. Open all apps and wait for load complete.
        // 2. Drag icon to homescreen.
        // 3. Verify that the icon works on homescreen.
        final AllApps allApps = mLauncher.getWorkspace().
                switchToAllApps();
        allApps.freeze();
        try {
            allApps.getAppIcon(APP_NAME).dragToWorkspace(false, false);
            mLauncher.getWorkspace().getWorkspaceAppIcon(APP_NAME).launch(getAppPackageName());
        } finally {
            allApps.unfreeze();
        }
        executeOnLauncher(launcher -> assertTrue(
                "Launcher activity is the top activity; expecting another activity to be the top "
                        + "one",
                isInBackground(launcher)));
    }

    @Test
    @PortraitLandscape
    public void testDragShortcut() throws Throwable {
        // 1. Open all apps and wait for load complete.
        // 2. Find the app and long press it to show shortcuts.
        // 3. Press icon center until shortcuts appear
        final AllApps allApps = mLauncher
                .getWorkspace()
                .switchToAllApps();
        allApps.freeze();
        try {
            final AppIconMenu menu = allApps
                    .getAppIcon(APP_NAME)
                    .openDeepShortcutMenu();
            final AppIconMenuItem menuItem0 = menu.getMenuItem(0);
            final AppIconMenuItem menuItem2 = menu.getMenuItem(2);

            final AppIconMenuItem menuItem;

            final String expectedShortcutName = "Shortcut 3";
            if (menuItem0.getText().equals(expectedShortcutName)) {
                menuItem = menuItem0;
            } else {
                final String shortcutName2 = menuItem2.getText();
                assertEquals("Wrong menu item", expectedShortcutName, shortcutName2);
                menuItem = menuItem2;
            }

            menuItem.dragToWorkspace(false, false);
            mLauncher.getWorkspace().getWorkspaceAppIcon(expectedShortcutName)
                    .launch(getAppPackageName());
        } finally {
            allApps.unfreeze();
        }
    }

    private AppIcon createShortcutIfNotExist(String name) {
        AppIcon appIcon = mLauncher.getWorkspace().tryGetWorkspaceAppIcon(name);
        if (appIcon == null) {
            AllApps allApps = mLauncher.getWorkspace().switchToAllApps();
            allApps.freeze();
            try {
                appIcon = allApps.getAppIcon(name);
                appIcon.dragToWorkspace(false, false);
            } finally {
                allApps.unfreeze();
            }
            appIcon = mLauncher.getWorkspace().getWorkspaceAppIcon(name);
        }
        return appIcon;
    }

    @Ignore("b/205014516")
    @Test
    @PortraitLandscape
    public void testDragToFolder() throws Exception {
        final AppIcon playStoreIcon = createShortcutIfNotExist("Play Store");
        final AppIcon gmailIcon = createShortcutIfNotExist("Gmail");

        FolderIcon folderIcon = gmailIcon.dragToIcon(playStoreIcon);

        Folder folder = folderIcon.open();
        folder.getAppIcon("Play Store");
        folder.getAppIcon("Gmail");
        Workspace workspace = folder.close();

        assertNull("Gmail should be moved to a folder.",
                workspace.tryGetWorkspaceAppIcon("Gmail"));
        assertNull("Play Store should be moved to a folder.",
                workspace.tryGetWorkspaceAppIcon("Play Store"));

        final AppIcon youTubeIcon = createShortcutIfNotExist("YouTube");

        folderIcon = youTubeIcon.dragToIcon(folderIcon);
        folder = folderIcon.open();
        folder.getAppIcon("YouTube");
        folder.close();
    }

    @Ignore("b/205027405")
    @Test
    @PortraitLandscape
    public void testPressBack() throws Exception {
        mLauncher.getWorkspace().switchToAllApps();
        mLauncher.pressBack();
        mLauncher.getWorkspace();
        waitForState("Launcher internal state didn't switch to Home", () -> LauncherState.NORMAL);

        AllApps allApps = mLauncher.getWorkspace().switchToAllApps();
        allApps.freeze();
        try {
            allApps.getAppIcon(APP_NAME).dragToWorkspace(false, false);
        } finally {
            allApps.unfreeze();
        }
        mLauncher.getWorkspace().getWorkspaceAppIcon(APP_NAME).launch(getAppPackageName());
        mLauncher.pressBack();
        mLauncher.getWorkspace();
        waitForState("Launcher internal state didn't switch to Home", () -> LauncherState.NORMAL);
    }

    @Test
    @PortraitLandscape
    public void testDeleteFromWorkspace() throws Exception {
        // test delete both built-in apps and user-installed app from workspace
        for (String appName : new String[] {"Gmail", "Play Store", APP_NAME}) {
            final AppIcon appIcon = createShortcutIfNotExist(appName);
            Workspace workspace = mLauncher.getWorkspace().deleteAppIcon(appIcon);
            assertNull(appName + " app was found after being deleted from workspace",
                    workspace.tryGetWorkspaceAppIcon(appName));
        }
    }

    public static String getAppPackageName() {
        return getInstrumentation().getContext().getPackageName();
    }
}
