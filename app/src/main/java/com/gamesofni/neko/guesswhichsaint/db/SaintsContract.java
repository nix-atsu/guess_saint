package com.gamesofni.neko.guesswhichsaint.db;


import android.provider.BaseColumns;
import com.gamesofni.neko.guesswhichsaint.R;

public final class SaintsContract {
    private SaintsContract() {}

    public static final int GENDER_MALE = 0;
    public static final int GENDER_FEMALE = 1;

    public static final String CATEGORY_MAGI = "magi";


    static class SaintEntry implements BaseColumns {
        static final String TABLE_NAME = "saints";
        public final static String _ID = BaseColumns._ID;

        public static final String ICON = "icon"; // drawables file name
        public static final String GENDER = "gender";
        public static final String CATEGORY = "category";
    }

    static class SaintTranslation implements BaseColumns {
        public static final int TABLE_NAME = R.string.saints_translation_table_name;

        public final static String TRANSLATION_ID = "translation_id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String ATTRIBUTES = "attributes";
        public static final String WIKI_LINK = "wikiLink";
    }

    // gonna be hardcoded for now
//    public static class AttributesEntry implements BaseColumns {
//        public static final String TABLE_NAME = "attributes";
//        public final static String _ID = BaseColumns._ID;
//        public static final String SAINT_ID = "saintId";
//        public static final String ATTRIBUTE = "attribute";
//    }
}

