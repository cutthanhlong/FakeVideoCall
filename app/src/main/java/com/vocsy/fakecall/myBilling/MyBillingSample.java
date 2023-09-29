package com.vocsy.fakecall.myBilling;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class MyBillingSample implements BillingClientStateListener {

    private Activity activity;
    private ArrayList<String> skuList = new ArrayList<>();
    private BillingClient billingClient;

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            int responseCode = billingResult.getResponseCode();

            if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    //handlePurchase(purchase);
                }
            } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {

            } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {

            }
        }
    };

    public MyBillingSample(Activity activity) {
        this.activity = activity;
        setUpBillingClient();

    }

    private void setUpBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();

        startConnection();
    }

    private void startConnection() {
        if (billingClient != null && !billingClient.isReady()) {
            billingClient.startConnection(this);
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        startConnection();
    }

    private void log(Object o) {
        Log.e("MyBilling", "log: => " + o);
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            log("billing connection is ready now");
            //makeQuery(AdsHandler.SKU_REMOVE_ADS);
        }
    }

    public void makeQuery(String sku, String type) {

        this.skuList.add(sku);

        if (billingClient.isReady()) {

            QueryProductDetailsParams queryProductDetailsParams =
                    QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                    ImmutableList.of(
                                            QueryProductDetailsParams.Product.newBuilder()
                                                    .setProductId(skuList.get(0))
                                                    .setProductType(type.equals("subs") ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP)
                                                    .build()))
                            .build();


            //BillingResult,  List<ProductDetails>

            billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, list) -> {
                // check BillingResult
                // process returned ProductDetails list

                if (list!=null) {
                    for (int pos = 0; pos < list.size(); pos++) {
                        if (list.get(pos).getTitle().equals(sku)) {
                            launchProductFlow(list.get(0));
                            break;
                        }
                    }
                }
            });
        } else {
            Toast.makeText(activity, "Please Wait", Toast.LENGTH_SHORT).show();
            startConnection();
        }
    }

    private void launchProductFlow(ProductDetails productDetails) {
        BillingFlowParams billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                                ImmutableList.of(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                // fetched via queryProductDetailsAsync
                                                .setProductDetails(productDetails)
                                                // to get an offer token, call ProductDetails.getOfferDetails()
                                                // for a list of offers that are available to the user
//                                                .setOfferToken()
                                                .build()
                                )
                        )
                        .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
    }
}