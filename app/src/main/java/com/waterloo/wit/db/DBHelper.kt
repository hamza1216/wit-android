package com.waterloo.wit.db


import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import com.waterloo.wit.MainApplication
import com.waterloo.wit.data.*
import com.waterloo.wit.helpers.WitHelper
import java.util.*

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ORGANIZATION_ENTRIES)
        db.execSQL(SQL_CREATE_BUILDING_ENTRIES)
        db.execSQL(SQL_CREATE_USER_ENTRIES)
        db.execSQL(SQL_CREATE_WORKITEM_ENTRIES)
        db.execSQL(SQL_CREATE_WORKITEM_PHOTO_ENTRIES)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ORGANIZATION_ENTRIES)
        db.execSQL(SQL_DELETE_BUILDING_ENTRIES)
        db.execSQL(SQL_DELETE_USER_ENTRIES)
        db.execSQL(SQL_DELETE_WORKITEM_ENTRIES)
        db.execSQL(SQL_DELETE_WORKITEM_PHOTO_ENTRIES)
        onCreate(db)
        MainApplication.instance.prefs.setDatabaseState(false)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
/*
    fun initDatabaseWithInitialData(){
        // insert organization
        insertOrganization(OrganizationItem(WitHelper.generateUniqId(), "Organization 1", "Alberta"))
        val organizationList =  readAllOrganization()
        if(organizationList.count()>0){
            val organizationItem = organizationList.get(0)
            insertBuilding(BuildingItem(WitHelper.generateUniqId(), organizationItem.UID, "Building 1", "448 Cooper St. Pintendre, QC G6C 9M3"))

        }
    }
    @Throws(SQLiteConstraintException::class)
    fun insertOrganization(organizationItem: OrganizationItem): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.OrganizationEntry.COLUMN_UID, organizationItem.UID)
        values.put(DBContract.OrganizationEntry.COLUMN_NAME, organizationItem.Name)
        values.put(DBContract.OrganizationEntry.COLUMN_PROVINCE, organizationItem.Province)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.OrganizationEntry.TABLE_NAME, null, values)

        return newRowId>0
    }

    @Throws(SQLiteConstraintException::class)
    fun deleteOrganization(organizationId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.OrganizationEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(organizationId)
        // Issue SQL statement.
        db.delete(DBContract.OrganizationEntry.TABLE_NAME, selection, selectionArgs)

        return true
    }

    fun readOrganization(organizationId: String): ArrayList<OrganizationItem> {
        val users = ArrayList<OrganizationItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.OrganizationEntry.TABLE_NAME + " WHERE " + DBContract.OrganizationEntry.COLUMN_UID + "='" + organizationId + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_ORGANIZATION_ENTRIES)
            return ArrayList()
        }

        var name: String
        var province: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                name = cursor.getString(cursor.getColumnIndex(DBContract.OrganizationEntry.COLUMN_NAME))
                province = cursor.getString(cursor.getColumnIndex(DBContract.OrganizationEntry.COLUMN_PROVINCE))

                users.add(OrganizationItem(organizationId, name, province))
                cursor.moveToNext()
            }
        }
        return users
    }

    fun readAllOrganization(): ArrayList<OrganizationItem> {
        val organizations = ArrayList<OrganizationItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.OrganizationEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_ORGANIZATION_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var name: String
        var province: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.OrganizationEntry.COLUMN_UID))
                name = cursor.getString(cursor.getColumnIndex(DBContract.OrganizationEntry.COLUMN_NAME))
                province = cursor.getString(cursor.getColumnIndex(DBContract.OrganizationEntry.COLUMN_PROVINCE))

                organizations.add(OrganizationItem(uid, name, province))
                cursor.moveToNext()
            }
        }
        return organizations
    }
    // building entries
    @Throws(SQLiteConstraintException::class)
    fun insertBuilding(buildingItem: BuildingItem): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.BuildingEntry.COLUMN_UID, buildingItem.UID)
        values.put(DBContract.BuildingEntry.COLUMN_ORGANIZATION_UID, buildingItem.Organization_UID)
        values.put(DBContract.BuildingEntry.COLUMN_NAME, buildingItem.Name)
        values.put(DBContract.BuildingEntry.COLUMN_ADDRESS, buildingItem.Address)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.BuildingEntry.TABLE_NAME, null, values)

        return newRowId>0
    }
    fun deleteBuilding(buildingId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.BuildingEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(buildingId)
        // Issue SQL statement.
        db.delete(DBContract.BuildingEntry.TABLE_NAME, selection, selectionArgs)

        return true
    }
    fun updateBuildingItem(buildingItem: BuildingItem): Boolean{
        // Gets the data repository in write mode
        val db = writableDatabase
        val selection = DBContract.BuildingEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(buildingItem.UID)
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.BuildingEntry.COLUMN_UID, buildingItem.UID)
        values.put(DBContract.BuildingEntry.COLUMN_ORGANIZATION_UID, buildingItem.Organization_UID)
        values.put(DBContract.BuildingEntry.COLUMN_NAME, buildingItem.Name)
        values.put(DBContract.BuildingEntry.COLUMN_ADDRESS, buildingItem.Address)
        val updatedRowId = db.update(DBContract.BuildingEntry.TABLE_NAME, values, selection, selectionArgs)
        return  updatedRowId>0
    }
    fun readBuilding(buildingId: String): ArrayList<BuildingItem> {
        val buildings = ArrayList<BuildingItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.BuildingEntry.TABLE_NAME + " WHERE " + DBContract.BuildingEntry.COLUMN_UID + "='" + buildingId + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_BUILDING_ENTRIES)
            return ArrayList()
        }

        var organization_UID: String
        var name: String
        var address: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                organization_UID = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_ORGANIZATION_UID))
                name = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_NAME))
                address = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_ADDRESS))
                buildings.add(BuildingItem(buildingId, organization_UID, name, address))
                cursor.moveToNext()
            }
        }
        return buildings
    }

    fun readAllBuildings(): ArrayList<BuildingItem> {
        val buildings = ArrayList<BuildingItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.BuildingEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_BUILDING_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var organization_UID: String
        var name: String
        var address: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_UID))
                organization_UID = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_ORGANIZATION_UID))
                name = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_NAME))
                address = cursor.getString(cursor.getColumnIndex(DBContract.BuildingEntry.COLUMN_ADDRESS))
                buildings.add(BuildingItem(uid, organization_UID, name, address))
                cursor.moveToNext()
            }
        }
        return buildings
    }
    // user entries
    @Throws(SQLiteConstraintException::class)
    fun insertUser(userItem: UserItem): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.UserEntry.COLUMN_UID, userItem.UID)
        values.put(DBContract.UserEntry.COLUMN_ORGANIZATION_UID, userItem.Organization_UID)
        values.put(DBContract.UserEntry.COLUMN_FIRSTNAME, userItem.FirstName)
        values.put(DBContract.UserEntry.COLUMN_LASTNAME, userItem.LastName)
        values.put(DBContract.UserEntry.COLUMN_EMAIL, userItem.Email)
        values.put(DBContract.UserEntry.COLUMN_PASSWORD, userItem.Password)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.UserEntry.TABLE_NAME, null, values)

        return newRowId>0
    }
    fun deleteUser(userId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.UserEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(userId)
        // Issue SQL statement.
        db.delete(DBContract.UserEntry.TABLE_NAME, selection, selectionArgs)
        return true
    }
    fun readUser(email: String, password: String): ArrayList<UserItem>{
        val buildings = ArrayList<UserItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.UserEntry.TABLE_NAME + " WHERE " + DBContract.UserEntry.COLUMN_EMAIL + "='" + email + "' AND " + DBContract.UserEntry.COLUMN_PASSWORD + "='" + password + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_USER_ENTRIES)
            return ArrayList()
        }

        var userId: String
        var organization_UID: String
        var firstName: String
        var lastName: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                userId = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_UID))
                organization_UID = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_ORGANIZATION_UID))
                firstName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_FIRSTNAME))
                lastName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_LASTNAME))
                buildings.add(UserItem(userId, organization_UID, firstName, lastName, email, password))
                cursor.moveToNext()
            }
        }
        return buildings
    }
    fun readUser(userId: String): ArrayList<UserItem> {
        val buildings = ArrayList<UserItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.UserEntry.TABLE_NAME + " WHERE " + DBContract.UserEntry.COLUMN_UID + "='" + userId + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_USER_ENTRIES)
            return ArrayList()
        }

        var organization_UID: String
        var firstName: String
        var lastName: String
        var email: String
        var password: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                organization_UID = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_ORGANIZATION_UID))
                firstName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_FIRSTNAME))
                lastName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_LASTNAME))
                email = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_EMAIL))
                password = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_PASSWORD))
                buildings.add(UserItem(userId, organization_UID, firstName, lastName, email, password))
                cursor.moveToNext()
            }
        }
        return buildings
    }

    fun readAllUsers(): ArrayList<UserItem> {
        val users = ArrayList<UserItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.UserEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_USER_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var organization_UID: String
        var firstName: String
        var lastName: String
        var email: String
        var password: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_UID))
                organization_UID = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_ORGANIZATION_UID))
                firstName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_FIRSTNAME))
                lastName = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_LASTNAME))
                email = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_EMAIL))
                password = cursor.getString(cursor.getColumnIndex(DBContract.UserEntry.COLUMN_PASSWORD))
                users.add(UserItem(uid, organization_UID, firstName, lastName, email, password))
                cursor.moveToNext()
            }
        }
        return users
    }
    // WorkItem entries
    @Throws(SQLiteConstraintException::class)
    fun insertWorkItem(workItem: WorkItem): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.WorkItemEntry.COLUMN_UID, workItem.UID)
        values.put(DBContract.WorkItemEntry.COLUMN_BUILDING_ID, workItem.Building_UID)
        values.put(DBContract.WorkItemEntry.COLUMN_WORKTITLE, workItem.WorkTitle)
        values.put(DBContract.WorkItemEntry.COLUMN_DATETIME, workItem.Date_Time)
        values.put(DBContract.WorkItemEntry.COLUMN_AREA, workItem.Area)
        values.put(DBContract.WorkItemEntry.COLUMN_NOTES, workItem.Notes)
        values.put(DBContract.WorkItemEntry.COLUMN_GPS_LATITUDE, workItem.GPS_Latitude)
        values.put(DBContract.WorkItemEntry.COLUMN_GPS_LONGITUDE, workItem.GPS_Longitude)
        values.put(DBContract.WorkItemEntry.COLUMN_FLOOR_LEVEL, workItem.FloorLevel)
        values.put(DBContract.WorkItemEntry.COLUMN_USER_ID, workItem.UserId)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.WorkItemEntry.TABLE_NAME, null, values)

        return newRowId>0
    }
    fun updateWorkItem(workItem: WorkItem): Boolean{
        // Gets the data repository in write mode
        val db = writableDatabase
        val selection = DBContract.WorkItemEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(workItem.UID)
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.WorkItemEntry.COLUMN_UID, workItem.UID)
        values.put(DBContract.WorkItemEntry.COLUMN_BUILDING_ID, workItem.Building_UID)
        values.put(DBContract.WorkItemEntry.COLUMN_WORKTITLE, workItem.WorkTitle)
        values.put(DBContract.WorkItemEntry.COLUMN_DATETIME, workItem.Date_Time)
        values.put(DBContract.WorkItemEntry.COLUMN_AREA, workItem.Area)
        values.put(DBContract.WorkItemEntry.COLUMN_NOTES, workItem.Notes)
        values.put(DBContract.WorkItemEntry.COLUMN_GPS_LATITUDE, workItem.GPS_Latitude)
        values.put(DBContract.WorkItemEntry.COLUMN_GPS_LONGITUDE, workItem.GPS_Longitude)
        values.put(DBContract.WorkItemEntry.COLUMN_FLOOR_LEVEL, workItem.FloorLevel)
        values.put(DBContract.WorkItemEntry.COLUMN_USER_ID, workItem.UserId)
        val updatedRowId = db.update(DBContract.WorkItemEntry.TABLE_NAME, values, selection, selectionArgs)
        return  updatedRowId>0
    }
    fun deleteWorkItem(workItemId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.WorkItemEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(workItemId)
        // Issue SQL statement.
        db.delete(DBContract.WorkItemEntry.TABLE_NAME, selection, selectionArgs)
        return true
    }

    fun readWorkItem(workItemId: String): ArrayList<WorkItem> {
        val workItems = ArrayList<WorkItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.WorkItemEntry.TABLE_NAME + " WHERE " + DBContract.WorkItemEntry.COLUMN_UID + "='" + workItemId + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_WORKITEM_ENTRIES)
            return ArrayList()
        }

        var building_UID: String
        var workTitle: String
        var dateTime: String
        var area: String
        var notes: String
        var gpsLatitude: String
        var gpsLongitude: String
        var floorLevel: Int
        var userId: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                building_UID = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_BUILDING_ID))
                workTitle = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_WORKTITLE))
                dateTime = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_DATETIME))
                area = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_AREA))
                notes = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_NOTES))
                gpsLatitude = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_GPS_LATITUDE))
                gpsLongitude = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_GPS_LONGITUDE))
                floorLevel = cursor.getInt(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_FLOOR_LEVEL))
                userId = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_USER_ID))
                workItems.add(WorkItem(workItemId, building_UID, workTitle, dateTime, area, notes, gpsLatitude, gpsLongitude, floorLevel, userId))
                cursor.moveToNext()
            }
        }
        return workItems
    }
    fun readWorkItemWithKeyword(text: String): ArrayList<WorkItem> {
        val workItems = ArrayList<WorkItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.WorkItemEntry.TABLE_NAME + " WHERE " +
                    DBContract.WorkItemEntry.COLUMN_WORKTITLE + " LIKE '%" + text + "%' OR " +
                    DBContract.WorkItemEntry.COLUMN_AREA + " LIKE '%" + text + "%' OR " +
                    DBContract.WorkItemEntry.COLUMN_NOTES + " LIKE '%" + text + "%'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_WORKITEM_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var building_UID: String
        var workTitle: String
        var dateTime: String
        var area: String
        var notes: String
        var gpsLatitude: String
        var gpsLongitude: String
        var floorLevel: Int
        var userId: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_UID))
                building_UID = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_BUILDING_ID))
                workTitle = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_WORKTITLE))
                dateTime = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_DATETIME))
                area = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_AREA))
                notes = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_NOTES))
                gpsLatitude = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_GPS_LATITUDE))
                gpsLongitude = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_GPS_LONGITUDE))
                floorLevel = cursor.getInt(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_FLOOR_LEVEL))
                userId = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_USER_ID))
                workItems.add(WorkItem(uid, building_UID, workTitle, dateTime, area, notes, gpsLatitude, gpsLongitude, floorLevel, userId))
                cursor.moveToNext()
            }
        }
        return workItems
    }
    fun readAllWorkItems(): ArrayList<WorkItem> {
        val workItems = ArrayList<WorkItem>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.WorkItemEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_WORKITEM_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var building_UID: String
        var workTitle: String
        var dateTime: String
        var area: String
        var notes: String
        var gpsLatitude: String
        var gpsLongitude: String
        var floorLevel: Int
        var userId: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_UID))
                building_UID = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_BUILDING_ID))
                workTitle = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_WORKTITLE))
                dateTime = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_DATETIME))
                area = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_AREA))
                notes = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_NOTES))
                gpsLatitude = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_GPS_LATITUDE))
                gpsLongitude = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_GPS_LONGITUDE))
                floorLevel = cursor.getInt(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_FLOOR_LEVEL))
                userId = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemEntry.COLUMN_USER_ID))
                workItems.add(WorkItem(uid, building_UID, workTitle, dateTime, area, notes, gpsLatitude, gpsLongitude, floorLevel, userId))
                cursor.moveToNext()
            }
        }
        return workItems
    }
    // WorkItemPhoto entries
    @Throws(SQLiteConstraintException::class)
    fun insertWorkItemPhoto(workItemPhoto: WorkItemPhoto): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(DBContract.WorkItemPhotoEntry.COLUMN_UID, workItemPhoto.UID)
        values.put(DBContract.WorkItemPhotoEntry.COLUMN_WORKITEM_UID, workItemPhoto.Work_Item_UID)
        values.put(DBContract.WorkItemPhotoEntry.COLUMN_PATH, workItemPhoto.Path)

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db.insert(DBContract.WorkItemPhotoEntry.TABLE_NAME, null, values)

        return newRowId>0
    }
    fun deleteWorkItemPhoto(workItemPhotoId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.WorkItemPhotoEntry.COLUMN_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(workItemPhotoId)
        // Issue SQL statement.
        db.delete(DBContract.WorkItemPhotoEntry.TABLE_NAME, selection, selectionArgs)
        return true
    }
    fun deleteAllPhotohWorkItemId(workItemId: String): Boolean {
        // Gets the data repository in write mode
        val db = writableDatabase
        // Define 'where' part of query.
        val selection = DBContract.WorkItemPhotoEntry.COLUMN_WORKITEM_UID + " LIKE ?"
        // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(workItemId)
        // Issue SQL statement.
        db.delete(DBContract.WorkItemPhotoEntry.TABLE_NAME, selection, selectionArgs)
        return true
    }
    fun readWorkItemPhoto(workItemId: String): ArrayList<WorkItemPhoto> {
        val workItemPhotos = ArrayList<WorkItemPhoto>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.WorkItemPhotoEntry.TABLE_NAME + " WHERE " + DBContract.WorkItemPhotoEntry.COLUMN_WORKITEM_UID + "='" + workItemId + "'", null)
        } catch (e: SQLiteException) {
            // if table not yet present, create it
            db.execSQL(SQL_CREATE_WORKITEM_PHOTO_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var path: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemPhotoEntry.COLUMN_UID))
                path = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemPhotoEntry.COLUMN_PATH))
                workItemPhotos.add(WorkItemPhoto(uid, workItemId, path))
                cursor.moveToNext()
            }
        }
        return workItemPhotos
    }

    fun readAllWorkItemPhotos(): ArrayList<WorkItemPhoto> {
        val workItemPhotos = ArrayList<WorkItemPhoto>()
        val db = writableDatabase
        var cursor: Cursor
        try {
            cursor = db.rawQuery("select * from " + DBContract.WorkItemPhotoEntry.TABLE_NAME, null)
        } catch (e: SQLiteException) {
            db.execSQL(SQL_CREATE_WORKITEM_PHOTO_ENTRIES)
            return ArrayList()
        }

        var uid: String
        var workItemId: String
        var path: String
        if (cursor.moveToFirst()) {
            while (cursor.isAfterLast == false) {
                uid = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemPhotoEntry.COLUMN_UID))
                workItemId = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemPhotoEntry.COLUMN_WORKITEM_UID))
                path = cursor.getString(cursor.getColumnIndex(DBContract.WorkItemPhotoEntry.COLUMN_PATH))
                workItemPhotos.add(WorkItemPhoto(uid, workItemId, path))
                cursor.moveToNext()
            }
        }
        return workItemPhotos
    }
 */
    companion object {
        // If you change the database schema, you must increment the database version.
        val DATABASE_VERSION = 2
        val DATABASE_NAME = "WITDatabase.db"

        private val SQL_CREATE_ORGANIZATION_ENTRIES =
            "CREATE TABLE " + DBContract.OrganizationEntry.TABLE_NAME + " (" +
                    DBContract.OrganizationEntry.COLUMN_UID + " TEXT PRIMARY KEY," +
                    DBContract.OrganizationEntry.COLUMN_NAME + " TEXT," +
                    DBContract.OrganizationEntry.COLUMN_PROVINCE + " TEXT)"

        private val SQL_DELETE_ORGANIZATION_ENTRIES = "DROP TABLE IF EXISTS " + DBContract.OrganizationEntry.TABLE_NAME

        private val SQL_CREATE_BUILDING_ENTRIES =
            "CREATE TABLE " + DBContract.BuildingEntry.TABLE_NAME + " (" +
                    DBContract.BuildingEntry.COLUMN_UID + " TEXT PRIMARY KEY," +
                    DBContract.BuildingEntry.COLUMN_ORGANIZATION_UID + " TEXT," +
                    DBContract.BuildingEntry.COLUMN_NAME + " TEXT," +
                    DBContract.BuildingEntry.COLUMN_ADDRESS + " TEXT)"

        private val SQL_DELETE_BUILDING_ENTRIES = "DROP TABLE IF EXISTS " + DBContract.BuildingEntry.TABLE_NAME

        private val SQL_CREATE_USER_ENTRIES =
            "CREATE TABLE " + DBContract.UserEntry.TABLE_NAME + " (" +
                    DBContract.UserEntry.COLUMN_UID + " TEXT PRIMARY KEY," +
                    DBContract.UserEntry.COLUMN_ORGANIZATION_UID + " TEXT," +
                    DBContract.UserEntry.COLUMN_FIRSTNAME + " TEXT," +
                    DBContract.UserEntry.COLUMN_LASTNAME + " TEXT," +
                    DBContract.UserEntry.COLUMN_EMAIL + " TEXT," +
                    DBContract.UserEntry.COLUMN_PASSWORD + " TEXT)"

        private val SQL_DELETE_USER_ENTRIES = "DROP TABLE IF EXISTS " + DBContract.UserEntry.TABLE_NAME

        private val SQL_CREATE_WORKITEM_ENTRIES =
            "CREATE TABLE " + DBContract.WorkItemEntry.TABLE_NAME + " (" +
                    DBContract.WorkItemEntry.COLUMN_UID + " TEXT PRIMARY KEY," +
                    DBContract.WorkItemEntry.COLUMN_BUILDING_ID + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_WORKTITLE + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_DATETIME + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_AREA + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_NOTES + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_GPS_LATITUDE + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_GPS_LONGITUDE + " TEXT," +
                    DBContract.WorkItemEntry.COLUMN_FLOOR_LEVEL + " INT," +
                    DBContract.WorkItemEntry.COLUMN_USER_ID + " TEXT)"

        private val SQL_DELETE_WORKITEM_ENTRIES = "DROP TABLE IF EXISTS " + DBContract.WorkItemEntry.TABLE_NAME

        private val SQL_CREATE_WORKITEM_PHOTO_ENTRIES =
            "CREATE TABLE " + DBContract.WorkItemPhotoEntry.TABLE_NAME + " (" +
                    DBContract.WorkItemPhotoEntry.COLUMN_UID + " TEXT PRIMARY KEY," +
                    DBContract.WorkItemPhotoEntry.COLUMN_WORKITEM_UID + " TEXT," +
                    DBContract.WorkItemPhotoEntry.COLUMN_PATH + " TEXT)"

        private val SQL_DELETE_WORKITEM_PHOTO_ENTRIES = "DROP TABLE IF EXISTS " + DBContract.WorkItemPhotoEntry.TABLE_NAME

    }

}