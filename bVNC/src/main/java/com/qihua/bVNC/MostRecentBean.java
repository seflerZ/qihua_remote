// This class was generated from com.qihua.bVNC.IMostRecentBean by a tool
// Do not edit this file directly! PLX THX
package com.qihua.bVNC;

public class MostRecentBean extends com.antlersoft.android.dbimpl.IdImplementationBase implements IMostRecentBean {

    public static final String GEN_TABLE_NAME = "MOST_RECENT";
    public static final int GEN_COUNT = 4;

    // Field constants
    public static final String GEN_FIELD__ID = "_id";
    public static final int GEN_ID__ID = 0;
    public static final String GEN_FIELD_CONNECTION_ID = "CONNECTION_ID";
    public static final int GEN_ID_CONNECTION_ID = 1;
    public static final String GEN_FIELD_SHOW_SPLASH_VERSION = "SHOW_SPLASH_VERSION";
    public static final int GEN_ID_SHOW_SPLASH_VERSION = 2;
    public static final String GEN_FIELD_TEXT_INDEX = "TEXT_INDEX";
    public static final int GEN_ID_TEXT_INDEX = 3;
    public static final com.antlersoft.android.dbimpl.NewInstance<MostRecentBean> GEN_NEW = new com.antlersoft.android.dbimpl.NewInstance<MostRecentBean>() {
        public MostRecentBean get() {
            return new MostRecentBean();
        }
    };
    // SQL Command for creating the table
    public static String GEN_CREATE = "CREATE TABLE MOST_RECENT (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "CONNECTION_ID INTEGER," +
            "SHOW_SPLASH_VERSION INTEGER," +
            "TEXT_INDEX INTEGER" +
            ")";
    // Members corresponding to defined fields
    private long gen__Id;
    private long gen_CONNECTION_ID;
    private long gen_SHOW_SPLASH_VERSION;
    private long gen_TEXT_INDEX;

    public String Gen_tableName() {
        return GEN_TABLE_NAME;
    }

    // Field accessors
    public long get_Id() {
        return gen__Id;
    }

    public void set_Id(long arg__Id) {
        gen__Id = arg__Id;
    }

    public long getConnectionId() {
        return gen_CONNECTION_ID;
    }

    public void setConnectionId(long arg_CONNECTION_ID) {
        gen_CONNECTION_ID = arg_CONNECTION_ID;
    }

    public long getShowSplashVersion() {
        return gen_SHOW_SPLASH_VERSION;
    }

    public void setShowSplashVersion(long arg_SHOW_SPLASH_VERSION) {
        gen_SHOW_SPLASH_VERSION = arg_SHOW_SPLASH_VERSION;
    }

    public long getTextIndex() {
        return gen_TEXT_INDEX;
    }

    public void setTextIndex(long arg_TEXT_INDEX) {
        gen_TEXT_INDEX = arg_TEXT_INDEX;
    }

    public android.content.ContentValues Gen_getValues() {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(GEN_FIELD__ID, Long.toString(this.gen__Id));
        values.put(GEN_FIELD_CONNECTION_ID, Long.toString(this.gen_CONNECTION_ID));
        values.put(GEN_FIELD_SHOW_SPLASH_VERSION, Long.toString(this.gen_SHOW_SPLASH_VERSION));
        values.put(GEN_FIELD_TEXT_INDEX, Long.toString(this.gen_TEXT_INDEX));
        return values;
    }

    /**
     * Return an array that gives the column index in the cursor for each field defined
     *
     * @param cursor Database cursor over some columns, possibly including this table
     * @return array of column indices; -1 if the column with that id is not in cursor
     */
    public int[] Gen_columnIndices(android.database.Cursor cursor) {
        int[] result = new int[GEN_COUNT];
        result[0] = cursor.getColumnIndex(GEN_FIELD__ID);
        // Make compatible with database generated by older version of plugin with uppercase column name
        if (result[0] == -1) {
            result[0] = cursor.getColumnIndex("_ID");
        }
        result[1] = cursor.getColumnIndex(GEN_FIELD_CONNECTION_ID);
        result[2] = cursor.getColumnIndex(GEN_FIELD_SHOW_SPLASH_VERSION);
        result[3] = cursor.getColumnIndex(GEN_FIELD_TEXT_INDEX);
        return result;
    }

    /**
     * Populate one instance from a cursor
     */
    public void Gen_populate(android.database.Cursor cursor, int[] columnIndices) {
        if (columnIndices[GEN_ID__ID] >= 0 && !cursor.isNull(columnIndices[GEN_ID__ID])) {
            gen__Id = cursor.getLong(columnIndices[GEN_ID__ID]);
        }
        if (columnIndices[GEN_ID_CONNECTION_ID] >= 0 && !cursor.isNull(columnIndices[GEN_ID_CONNECTION_ID])) {
            gen_CONNECTION_ID = cursor.getLong(columnIndices[GEN_ID_CONNECTION_ID]);
        }
        if (columnIndices[GEN_ID_SHOW_SPLASH_VERSION] >= 0 && !cursor.isNull(columnIndices[GEN_ID_SHOW_SPLASH_VERSION])) {
            gen_SHOW_SPLASH_VERSION = cursor.getLong(columnIndices[GEN_ID_SHOW_SPLASH_VERSION]);
        }
        if (columnIndices[GEN_ID_TEXT_INDEX] >= 0 && !cursor.isNull(columnIndices[GEN_ID_TEXT_INDEX])) {
            gen_TEXT_INDEX = cursor.getLong(columnIndices[GEN_ID_TEXT_INDEX]);
        }
    }

    /**
     * Populate one instance from a ContentValues
     */
    public void Gen_populate(android.content.ContentValues values) {
        gen__Id = values.getAsLong(GEN_FIELD__ID);
        gen_CONNECTION_ID = values.getAsLong(GEN_FIELD_CONNECTION_ID);
        gen_SHOW_SPLASH_VERSION = values.getAsLong(GEN_FIELD_SHOW_SPLASH_VERSION);
        gen_TEXT_INDEX = values.getAsLong(GEN_FIELD_TEXT_INDEX);
    }
}
