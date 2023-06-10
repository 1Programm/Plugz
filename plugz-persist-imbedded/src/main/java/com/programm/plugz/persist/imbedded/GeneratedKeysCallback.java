package com.programm.plugz.persist.imbedded;

import java.sql.ResultSet;
import java.sql.SQLException;

interface GeneratedKeysCallback {

    void call(ResultSet keys) throws SQLException;

}
