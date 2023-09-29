package com.vocsy.fakecall.myBilling;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.common.collect.ImmutableList;
import com.vocsy.fakecall.BuildConfig;
import com.vocsy.fakecall.R;

import java.util.List;

import vocsy.ads.AdsHandler;

public class MyBilling extends AppCompatActivity implements BillingClientStateListener {

    private Button purchase;
    private ProductDetails mProductDetails;
    private TextView price;

    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
            int responseCode = billingResult.getResponseCode();

            if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                }
            } else if (responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                AdsHandler.setPurchase(true);
                AdsHandler.setAdsOn(false);
                Toast.makeText(MyBilling.this, "Already", Toast.LENGTH_SHORT).show();
            } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                AdsHandler.setPurchase(false);
                AdsHandler.setAdsOn(true);
                Toast.makeText(MyBilling.this, "Cancel Request Success", Toast.LENGTH_SHORT).show();
            }
        }
    };

    void handlePurchase(Purchase purchase) {
        // Purchase retrieved from BillingClient#queryPurchasesAsync or your PurchasesUpdatedListener.

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    AdsHandler.setPurchase(true);
                    AdsHandler.setAdsOn(false);
                }
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    private BillingClient billingClient;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);
        purchase = findViewById(R.id.purchase);
        price = findViewById(R.id.price);

        setUpBillingClient();

        purchase.setOnClickListener(view -> {

            if (mProductDetails != null) {
                if (BuildConfig.APPLICATION_ID.equals("com.vocsy.fakecall")) {
                    launchProductFlow(mProductDetails);
                } else {
                    Toast.makeText(this, "This feature not available for this package", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Something Want Wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUpBillingClient() {
        billingClient = BillingClient.newBuilder(this)
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

    private void makeQuery(String sku) {

        if (billingClient.isReady()) {
            QueryProductDetailsParams queryProductDetailsParams =
                    QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                    ImmutableList.of(
                                            QueryProductDetailsParams.Product.newBuilder()
                                                    .setProductId(sku)
                                                    .setProductType(BillingClient.ProductType.INAPP)
                                                    .build()))
                            .build();
            //BillingResult,  List<ProductDetails>

            billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult, list) -> {
                // check BillingResult
                // process returned ProductDetails list

                if (list != null && list.size() > 0) {
                    mProductDetails = list.get(0);

                    if (mProductDetails != null) {
                        price.setText(
                                mProductDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode()
                                        + " " + mProductDetails.getOneTimePurchaseOfferDetails().getFormattedPrice()
                        );
                    } else {
                        price.setText("Unable to find Price");
                    }


                }
            });
        } else {
            Toast.makeText(this, "Please Wait", Toast.LENGTH_SHORT).show();
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

        BillingResult billingResult = billingClient.launchBillingFlow(MyBilling.this, billingFlowParams);

    }

    @Override
    public void onBillingServiceDisconnected() {
        startConnection();
    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            log("billing connection ready: => ");
            makeQuery(AdsHandler.SKU_REMOVE_ADS);
        }
    }

    private void log(Object o) {
        Log.e("TAG", "urvish log: " + o);
    }
}

