/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the license at usr/src/OPENSOLARIS.LICENSE
 * or http://www.opensolaris.org/os/licensing.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at usr/src/OPENSOLARIS.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
package com.sun;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import junit.framework.TestCase;
import org.jvnet.solaris.libzfs.LibZFS;
import org.jvnet.solaris.libzfs.ZFSObject;
import org.jvnet.solaris.libzfs.ZFSPool;
import org.jvnet.solaris.libzfs.ZFSType;
import org.jvnet.solaris.libzfs.jna.zfs_prop_t;
import org.jvnet.solaris.libzfs.jna.zpool_prop_t;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

    private static final String ZFS_TEST_POOL_OVERRIDE_PROPERTY = "libzfs.test.pool";

    private static final String ZFS_TEST_POOL_BASENAME_DEFAULT = "rpool/kohsuke/";

    private String ZFS_TEST_POOL_BASENAME;

    private String tearDownDataSet;
    private boolean tearDownAutoDestroy = false;

    public void setUp() throws Exception {
        super.setUp();

        /* allows override of zfs pool used in testing */
        ZFS_TEST_POOL_BASENAME = System
                .getProperty(ZFS_TEST_POOL_OVERRIDE_PROPERTY,
                        ZFS_TEST_POOL_BASENAME_DEFAULT);
    }

    public void tearDown() throws Exception {
        super.tearDown();

        final LibZFS zfs = new LibZFS();

        if (tearDownAutoDestroy && tearDownDataSet != null
                && !"".equals(tearDownDataSet)) {
            System.out.println("TearDown test dataset [" + tearDownDataSet
                    + "]");

            if (zfs.exists(tearDownDataSet)) {
                final ZFSObject fs = zfs.open(tearDownDataSet);
                fs.unshare();
                fs.unmount();
                fs.destory();
            }
        }
    }

    public void defineTestTearDown(final String dataSet,
            final boolean autoDestroy) {
        tearDownDataSet = dataSet;
        tearDownAutoDestroy = autoDestroy;
    }

    public String defineTestDataSetName() {
        final String dataSet = ZFS_TEST_POOL_BASENAME + getName();
        System.out.println("Test DataSet [" + dataSet + "]");
        return dataSet;
    }

    public void testApp() {
        LibZFS zfs = new LibZFS();

        System.out.println("Iterating roots");
        for (ZFSPool pool : zfs.roots()) {
            System.out.println(pool.getName());
            for (ZFSObject child : pool.children()) {
                System.out.println("- " + child.getName());
            }
        }
    }

    public void testGetFilesystemTree() {
        LibZFS zfs = new LibZFS();
        // List<ZFSPool> pools = zfs.roots();
        // if ( pools.size() > 0 ) {
        // ZFSObject filesystem = pools.get(0);
        ZFSObject filesystem = zfs.open("rpool");
        if (filesystem != null) {
            System.out.println("single tree: " + filesystem.getName());
            List<ZFSObject> clist = new ArrayList<ZFSObject>();
            for (ZFSObject child : filesystem.children(clist, filesystem)) {
                if (child.getName().contains("@")) {
                    System.out.println("snapshot  :" + child.getName());
                } else {
                    System.out.println("child     :" + child.getName());
                }
            }
        } else {
            System.out.println("no zfs pools were found");
        }
    }

    public void testCreate() {
        LibZFS zfs = new LibZFS();

        final String dataSet = ZFS_TEST_POOL_BASENAME + "testCreate"
                + System.currentTimeMillis();

        assertFalse("Prerequisite Failed, DataSet already exists [" + dataSet
                + "] ", zfs.exists(dataSet));

        try {
            ZFSObject fs = zfs.create(dataSet, ZFSType.FILESYSTEM);

            assertNotNull("ZFSObject was null for DataSet [" + dataSet + "]",
                    fs);
            assertEquals("ZFSObject doesn't match name specified at create",
                    dataSet, fs.getName());
            assertTrue("ZFS exists doesn't report ZFS's creation", zfs
                    .exists(dataSet));

        } finally {
            ZFSObject fs = zfs.open(dataSet);
            fs.destory();

            assertFalse("Tidy Up Failed, DataSet still exists [" + dataSet
                    + "] ", zfs.exists(dataSet));
        }
    }

    public void testDestroy() {
        LibZFS zfs = new LibZFS();

        final String dataSet = ZFS_TEST_POOL_BASENAME + "testDestroy"
                + System.currentTimeMillis();

        zfs.create(dataSet, ZFSType.FILESYSTEM);

        assertTrue("Prerequisite Failed, Test DataSet [" + dataSet
                + "] didn't create", zfs.exists(dataSet));

        try {
            ZFSObject fs = zfs.open(dataSet);

            assertNotNull("ZFSObject was null for DataSet [" + dataSet + "]",
                    fs);
            assertEquals("ZFSObject doesn't match name specified at open",
                    dataSet, fs.getName());
            assertTrue("ZFS exists doesn't report ZFS", zfs.exists(dataSet));

            fs.destory();

            assertFalse("ZFS exists doesn't report ZFS as destroyed", zfs
                    .exists(dataSet));

        } finally {
            assertFalse("Tidy Up Failed, DataSet still exists [" + dataSet
                    + "] ", zfs.exists(dataSet));
        }
    }

    public void testUserProperty() {
        LibZFS zfs = new LibZFS();
        zfs.create(ZFS_TEST_POOL_BASENAME + "testUserProperty",
                ZFSType.FILESYSTEM);

        ZFSObject o = zfs.open(ZFS_TEST_POOL_BASENAME + "testUserProperty");
        String property = "my:test";
        o.setProperty(property, String.valueOf(System.currentTimeMillis()));
        System.out.println("Property " + property + " is "
                + o.getUserProperty(property));
    }

    public void testGetZfsProperties() {
        LibZFS zfs = new LibZFS();

        List<zfs_prop_t> props = new ArrayList<zfs_prop_t>();
        for (zfs_prop_t prop : EnumSet.allOf(zfs_prop_t.class)) {
            props.add(prop);
        }

        for (ZFSPool pool : zfs.roots()) {
            System.out.println("pool    :" + pool.getName());

            Hashtable<zfs_prop_t, String> zfsPoolProps = pool
                    .getZfsProperty(props);
            for (zfs_prop_t prop : zfsPoolProps.keySet()) {
                System.out.println("zfs_prop_t " + prop + "(" + prop.ordinal()
                        + ") = " + zfsPoolProps.get(prop));
            }
        }

        ZFSObject o = zfs.open("rpool/kohsuke");
        System.out.println("pool    :" + o.getName());

        Hashtable<zfs_prop_t, String> zfsPoolProps = o.getZfsProperty(props);
        for (zfs_prop_t prop : zfsPoolProps.keySet()) {
            System.out.println("zfs_prop_t " + prop + "(" + prop.ordinal()
                    + ") = " + zfsPoolProps.get(prop));
        }
    }

    public void testGetZpoolProperties() {
        LibZFS zfs = new LibZFS();

        for (ZFSPool pool : zfs.roots()) {
            ZFSObject o = zfs.open(pool.getName());
            System.out.println("name:" + o.getName() + " size:"
                    + o.getZpoolProperty(zpool_prop_t.ZPOOL_PROP_SIZE)
                    + " used:"
                    + o.getZpoolProperty(zpool_prop_t.ZPOOL_PROP_USED));
        }
    }

    public void testInheritProperty() {
        LibZFS zfs = new LibZFS();
        zfs.create(ZFS_TEST_POOL_BASENAME + "testInheritProperty",
                ZFSType.FILESYSTEM);
        zfs.create(ZFS_TEST_POOL_BASENAME + "testInheritProperty/child",
                ZFSType.FILESYSTEM);

        ZFSObject o = zfs.open(ZFS_TEST_POOL_BASENAME + "testInheritProperty");
        String property = "my:test";
        o.setProperty(property, String.valueOf(System.currentTimeMillis()));
        System.out.println("set test: Property " + property + " is "
                + o.getUserProperty(property));
        o.inheritProperty(property);

        ZFSObject o2 = zfs.open(ZFS_TEST_POOL_BASENAME
                + "testInheritProperty/child");
        System.out.println("inherit test: Property " + property + " is "
                + o2.getUserProperty(property));
    }

    public void test_zfsObject_exists() {
        final String dataSet = defineTestDataSetName();
        defineTestTearDown(dataSet, true);

        final LibZFS zfs = new LibZFS();

        // Prerequisite
        assertFalse("Prerequisite Failed, DataSet already exists [" + dataSet
                + "] ", zfs.exists(dataSet));

        final ZFSObject fs1 = zfs.create(dataSet, ZFSType.FILESYSTEM);

        assertNotNull("Prerequisite Failed ZFS dataset created was null ["
                + dataSet + "]", fs1);

        assertTrue("ZFS exists failed for freshly created dataset", zfs
                .exists(dataSet));
        assertTrue("ZFS exists failed for freshly created dataset", zfs.exists(
                dataSet, ZFSType.FILESYSTEM));

        fs1.destory();
        assertFalse("ZFS exists failed for freshly destory dataset", zfs
                .exists(dataSet));
        assertFalse("ZFS exists failed for freshly destory dataset", zfs
                .exists(dataSet, ZFSType.FILESYSTEM));

        final ZFSObject fs2 = zfs.create(dataSet, ZFSType.FILESYSTEM);

        assertNotNull("Prerequisite Failed ZFS dataset created was null ["
                + dataSet + "]", fs2);

        assertTrue("ZFS exists failed for freshly created dataset", zfs
                .exists(dataSet));
        assertTrue("ZFS exists failed for freshly created dataset", zfs.exists(
                dataSet, ZFSType.FILESYSTEM));

        fs2.destory();
        assertFalse("ZFS exists failed for freshly destory dataset", zfs
                .exists(dataSet));
        assertFalse("ZFS exists failed for freshly destory dataset", zfs
                .exists(dataSet, ZFSType.FILESYSTEM));
    }

    public void test_zfsObject_isMounted() {
        final String dataSet = defineTestDataSetName();
        defineTestTearDown(dataSet, true);

        final LibZFS zfs = new LibZFS();

        // Prerequisite
        assertFalse("Prerequisite Failed, DataSet already exists [" + dataSet
                + "] ", zfs.exists(dataSet));

        final ZFSObject fs = zfs.create(dataSet, ZFSType.FILESYSTEM);

        assertNotNull("Prerequisite Failed ZFS dataset created was null ["
                + dataSet + "]", fs);

        assertFalse("ZFS spec does not have dataset mounted at create", fs
                .isMounted());

        fs.mount();
        assertTrue("ZFS dataset mount failed, or isMounted failed", fs
                .isMounted());

        fs.unmount();
        assertFalse("ZFS dataset unmount failed, or isMounted failed", fs
                .isMounted());

        fs.mount();
        assertTrue("ZFS dataset mount failed, or isMounted failed", fs
                .isMounted());

        fs.unmount();
        assertFalse("ZFS dataset unmount failed, or isMounted failed", fs
                .isMounted());
    }

    public void xtest_zfsObject_isShared() {
        final String dataSet = defineTestDataSetName();
        defineTestTearDown(dataSet, true);

        final LibZFS zfs = new LibZFS();

        // Prerequisite
        assertFalse("Prerequisite Failed, DataSet already exists [" + dataSet
                + "] ", zfs.exists(dataSet));

        final ZFSObject fs = zfs.create(dataSet, ZFSType.FILESYSTEM);

        assertNotNull("Prerequisite Failed ZFS dataset created was null ["
                + dataSet + "]", fs);

        assertFalse("ZFS spec does not have dataset shared at create", fs
                .isShared());

        fs.share();
        assertTrue("ZFS dataset share failed, or isShared failed", fs
                .isShared());

        fs.unshare();
        assertFalse("ZFS dataset unshare failed, or isShared failed", fs
                .isShared());

        fs.share();
        assertTrue("ZFS dataset share failed, or isShared failed", fs
                .isShared());

        fs.unshare();
        assertFalse("ZFS dataset unshare failed, or isShared failed", fs
                .isShared());
    }

}