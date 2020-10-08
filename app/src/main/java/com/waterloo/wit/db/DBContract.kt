package com.waterloo.wit.db

import android.provider.BaseColumns

object DBContract {

    /* Inner class that defines the table contents */
    class OrganizationEntry : BaseColumns {
        companion object {
            // Organization Table 3 Columns
            val TABLE_NAME = "Organization"
            val COLUMN_UID = "UID"
            val COLUMN_NAME = "Name"
            val COLUMN_PROVINCE = "Province"
        }
    }
    class BuildingEntry : BaseColumns{
        companion object {
            // Building Table 4 Columns
            val TABLE_NAME = "Building"
            val COLUMN_UID = "UID"
            val COLUMN_ORGANIZATION_UID = "Organization_UID"
            val COLUMN_NAME = "Name"
            val COLUMN_ADDRESS = "Address"
        }
    }
    class UserEntry: BaseColumns{
        companion object {
            // User Table 6 Columns
            val TABLE_NAME = "User"
            val COLUMN_UID = "UID"
            val COLUMN_ORGANIZATION_UID = "Organization_UID"
            val COLUMN_FIRSTNAME = "FirstName"
            val COLUMN_LASTNAME = "LastName"
            val COLUMN_EMAIL = "Email"
            val COLUMN_PASSWORD = "Password"
        }
    }
    class WorkItemEntry: BaseColumns{
        companion object {
            // WorkItem Table 6 Columns
            val TABLE_NAME = "Work_Item"
            val COLUMN_UID = "UID"
            val COLUMN_BUILDING_ID = "Building_UID"
            val COLUMN_WORKTITLE = "Work_Title"
            val COLUMN_DATETIME = "Date_Time"
            val COLUMN_AREA = "Area"
            val COLUMN_NOTES = "Notes"
            val COLUMN_GPS_LATITUDE = "GPS_Latitude"
            val COLUMN_GPS_LONGITUDE = "GPS_Longtidue"
            val COLUMN_FLOOR_LEVEL = "FLoorLevel"
            val COLUMN_USER_ID = "UserId"
        }
    }
    class WorkItemPhotoEntry: BaseColumns{
        companion object {
            // WorkItem Photo Table 3 Columns
            val TABLE_NAME = "Work_Item_Photo"
            val COLUMN_UID = "UID"
            val COLUMN_WORKITEM_UID = "Work_Item_UID"
            val COLUMN_PATH = "Path"
        }
    }
}