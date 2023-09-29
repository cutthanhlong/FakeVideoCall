package vocsy.ads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


public class AdsHandler {
    /**
     * always remember to remove this test id's, after complete app.
     */
    public static AdsHandler instance;
    public static String bannerId = "";
    public static String nativeId = "";
    public static String interstitialId = "";
    public static String openAds = "";

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;

    public static boolean isPurchaseModule = false;
    public static String license_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhIhaCffNji083RnGa43zP3dLsKwI+2IT/go6Bm5rKIclnHBam5+BrYuYEVkH35Y4WLpG7gJ2MNhTa8Etdi5ElTiLyWK8zIcqRlsEb7mpe59hktF/0FOQDIj5kJuI3siUo4Ko188w0u2WewNsBwXJwtsGrjkd+n7+eARExLSrnjhmZpRVqWp7bU9GrIXqejgEVUT0ilK2ZHvdGztcB6YQ6WL5MOsULg7CQOrm4WFitbLElJQ42Wo8cKj8R5In5AS24l83jyRQlDCdhxvg+g4lE0ZpXlKrzl7O70zv716S3ZkzV4yijmeKeMA2c8FclbMyUY4mZLYzcW/6pPBtm8En1QIDAQAB";
    public static String SKU_REMOVE_ADS = "remove_ads";

    @SuppressLint("StaticFieldLeak")
    private static Activity activity;

    public AdsHandler() {
    }

    public static void setAdsOn(boolean on) {
        editor.putBoolean("ads", on);
        editor.apply();
    }

    public static boolean isAdsOn() {

        if (!AdsHandler.isPurchaseModule) {
            return true;
        }

        return sharedPreferences != null && sharedPreferences.getBoolean("ads", true);
    }

    public static boolean isPurchased() {
        return sharedPreferences.getBoolean("isPurchased", false);
    }

    public static void setPurchase(boolean isPurchase) {
        editor.putBoolean("isPurchased", isPurchase);
        editor.apply();
    }

    @SuppressLint("CommitPrefEdits")
    public static synchronized AdsHandler getInstance(Activity activity) {
        AdsHandler.activity = activity;
        sharedPreferences = activity.getSharedPreferences("AdmobPref", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        if (instance == null) {
            instance = new AdsHandler();
        }

        return instance;
    }
}
