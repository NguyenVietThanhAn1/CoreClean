package com.coreclean.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coreclean.app.data.local.migration.MIGRATION_1_2
import com.coreclean.app.data.local.migration.MIGRATION_2_3
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO scan_results (filePath, fileSize, fileType, lastModified) VALUES ('/test/file.jpg', 1024, 'image/jpeg', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        val cursor = db.query("SELECT COUNT(*) FROM scan_results")
        cursor.moveToFirst()
        assert(cursor.getInt(0) == 1)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO scan_results (filePath, fileSize, fileType, lastModified) VALUES ('/test/file.jpg', 1024, 'image/jpeg', 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        val cursor = db.query("SELECT COUNT(*) FROM scan_results")
        cursor.moveToFirst()
        assert(cursor.getInt(0) == 1)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_2, MIGRATION_2_3)
    }
}
