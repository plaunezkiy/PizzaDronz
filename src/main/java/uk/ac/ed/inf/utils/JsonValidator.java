package uk.ac.ed.inf.utils;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class JsonValidator {
    /**
     * ensures a string is a valid JSON object, throws Exception if not valid
     * @param object string to test
     * @return boolean, object is valid JSON
     */
    public static boolean isValidJson(String object) {
        try {
            JsonParser.parseString(object);
        } catch (JsonSyntaxException e) {
            return false;
        }
        return true;
    }
}
