package com.trackingplan.examples.firebase;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.trackingplan.client.sdk.Trackingplan;

public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("FieldCanBeLocal")
    private final boolean triggerFirebaseEvents = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Trackingplan.init("YOUR_TP_ID")
                // .environment("PRODUCTION")
                // .sourceAlias("Android Example")
                // .customDomains(customDomains)
                // .ignoreContext()
                .enableDebug()
                .dryRun()
                .start(this);

        if (triggerFirebaseEvents) {
            var fa = FirebaseAnalytics.getInstance(this);
            new TriggerFirebaseEvents(fa).run();
        }
    }

    public static class TriggerFirebaseEvents implements Runnable {

        final private FirebaseAnalytics mFirebase;

        private final Bundle itemJeggings = new Bundle();
        private final Bundle itemBoots = new Bundle();
        private final Bundle itemSocks = new Bundle();

        public TriggerFirebaseEvents(FirebaseAnalytics firebase) {

            mFirebase = firebase;

            itemJeggings.putString(FirebaseAnalytics.Param.ITEM_ID, "SKU_123");
            itemJeggings.putString(FirebaseAnalytics.Param.ITEM_NAME, "jeggings");
            itemJeggings.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "pants");
            itemJeggings.putString(FirebaseAnalytics.Param.ITEM_VARIANT, "black");
            itemJeggings.putString(FirebaseAnalytics.Param.ITEM_BRAND, "Google");
            itemJeggings.putDouble(FirebaseAnalytics.Param.PRICE, 9.99);

            itemBoots.putString(FirebaseAnalytics.Param.ITEM_ID, "SKU_456");
            itemBoots.putString(FirebaseAnalytics.Param.ITEM_NAME, "boots");
            itemBoots.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "shoes");
            itemBoots.putString(FirebaseAnalytics.Param.ITEM_VARIANT, "brown");
            itemBoots.putString(FirebaseAnalytics.Param.ITEM_BRAND, "Google");
            itemBoots.putDouble(FirebaseAnalytics.Param.PRICE, 24.99);

            itemSocks.putString(FirebaseAnalytics.Param.ITEM_ID, "SKU_789");
            itemSocks.putString(FirebaseAnalytics.Param.ITEM_NAME, "ankle_socks");
            itemSocks.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "socks");
            itemSocks.putString(FirebaseAnalytics.Param.ITEM_VARIANT, "red");
            itemSocks.putString(FirebaseAnalytics.Param.ITEM_BRAND, "Google");
            itemSocks.putDouble(FirebaseAnalytics.Param.PRICE, 5.99);
        }

        @Override
        public void run() {

            mFirebase.setDefaultEventParameters(makeDefaultEventParams());

            selectProductFromList();
            viewProductDetails();
            addProductToWishList();
            viewCart();
            removeProductFromCart();
            beginCheckout();
            addShipingInfo();
            addPaymentInfo();
            purchase();
            refund();
            viewPromotion();
            selectPromotion();
            sendCustomEvent();
        }

        private Bundle makeDefaultEventParams() {
            Bundle defaultParams = new Bundle();
            defaultParams.putString("app_name", "FirebaseExample");
            defaultParams.putString("app_version", "0.0.1");
            return defaultParams;
        }

        private void selectProductFromList() {
            Bundle itemJeggingsWithIndex = new Bundle(itemJeggings);
            itemJeggingsWithIndex.putLong(FirebaseAnalytics.Param.INDEX, 1);

            Bundle itemBootsWithIndex = new Bundle(itemBoots);
            itemBootsWithIndex.putLong(FirebaseAnalytics.Param.INDEX, 2);

            Bundle itemSocksWithIndex = new Bundle(itemSocks);
            itemSocksWithIndex.putLong(FirebaseAnalytics.Param.INDEX, 3);

            Bundle viewItemListParams = new Bundle();
            viewItemListParams.putString(FirebaseAnalytics.Param.ITEM_LIST_ID, "L001");
            viewItemListParams.putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, "Related products");
            viewItemListParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsWithIndex, itemBootsWithIndex, itemSocksWithIndex });

            mFirebase.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST, viewItemListParams);
        }

        private void viewProductDetails() {
            Bundle viewItemParams = new Bundle();
            viewItemParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            viewItemParams.putDouble(FirebaseAnalytics.Param.VALUE, 9.99);
            viewItemParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[] { itemJeggings });

            mFirebase.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, viewItemParams);
        }

        private void addProductToWishList() {
            Bundle itemJeggingsWishlist = new Bundle(itemJeggings);
            itemJeggingsWishlist.putLong(FirebaseAnalytics.Param.QUANTITY, 2);

            Bundle addToWishlistParams = new Bundle();
            addToWishlistParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            addToWishlistParams.putDouble(FirebaseAnalytics.Param.VALUE, 2 * 9.99);
            addToWishlistParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsWishlist });

            mFirebase.logEvent(FirebaseAnalytics.Event.ADD_TO_WISHLIST, addToWishlistParams);
        }

        private void viewCart() {
            Bundle itemJeggingsCart = new Bundle(itemJeggings);
            itemJeggingsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 2);

            Bundle itemBootsCart = new Bundle(itemBoots);
            itemBootsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 1);

            Bundle viewCartParams = new Bundle();
            viewCartParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            viewCartParams.putDouble(FirebaseAnalytics.Param.VALUE, (1 * 9.99) + (1 * 24.99));
            viewCartParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsCart, itemBootsCart });

            mFirebase.logEvent(FirebaseAnalytics.Event.VIEW_CART, viewCartParams);
        }

        private void removeProductFromCart() {
            Bundle itemBootsCart = new Bundle(itemBoots);
            itemBootsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 1);

            Bundle removeCartParams = new Bundle();
            removeCartParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            removeCartParams.putDouble(FirebaseAnalytics.Param.VALUE, (1 * 24.99));
            removeCartParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemBootsCart });

            mFirebase.logEvent(FirebaseAnalytics.Event.REMOVE_FROM_CART, removeCartParams);
        }

        private void beginCheckout() {

            Bundle itemJeggingsCart = new Bundle(itemJeggings);
            itemJeggingsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 2);

            Bundle beginCheckoutParams = new Bundle();
            beginCheckoutParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            beginCheckoutParams.putDouble(FirebaseAnalytics.Param.VALUE, 14.98);
            beginCheckoutParams.putString(FirebaseAnalytics.Param.COUPON, "SUMMER_FUN");
            beginCheckoutParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsCart });

            mFirebase.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, beginCheckoutParams);
        }

        private void addShipingInfo() {

            Bundle itemJeggingsCart = new Bundle(itemJeggings);
            itemJeggingsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 2);

            Bundle addShippingParams = new Bundle();
            addShippingParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            addShippingParams.putDouble(FirebaseAnalytics.Param.VALUE, 14.98);
            addShippingParams.putString(FirebaseAnalytics.Param.COUPON, "SUMMER_FUN");
            addShippingParams.putString(FirebaseAnalytics.Param.SHIPPING_TIER, "Ground");
            addShippingParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsCart });

            mFirebase.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO, addShippingParams);
        }

        private void addPaymentInfo() {

            Bundle itemJeggingsCart = new Bundle(itemJeggings);
            itemJeggingsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 2);

            Bundle addPaymentParams = new Bundle();
            addPaymentParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            addPaymentParams.putDouble(FirebaseAnalytics.Param.VALUE, 14.98);
            addPaymentParams.putString(FirebaseAnalytics.Param.COUPON, "SUMMER_FUN");
            addPaymentParams.putString(FirebaseAnalytics.Param.PAYMENT_TYPE, "Visa");
            addPaymentParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsCart });

            mFirebase.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO, addPaymentParams);
        }

        private void purchase() {

            Bundle itemJeggingsCart = new Bundle(itemJeggings);
            itemJeggingsCart.putLong(FirebaseAnalytics.Param.QUANTITY, 2);

            Bundle purchaseParams = new Bundle();
            purchaseParams.putString(FirebaseAnalytics.Param.TRANSACTION_ID, "T12345");
            purchaseParams.putString(FirebaseAnalytics.Param.AFFILIATION, "Google Store");
            purchaseParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            purchaseParams.putDouble(FirebaseAnalytics.Param.VALUE, 14.98);
            purchaseParams.putDouble(FirebaseAnalytics.Param.TAX, 2.58);
            purchaseParams.putDouble(FirebaseAnalytics.Param.SHIPPING, 5.34);
            purchaseParams.putString(FirebaseAnalytics.Param.COUPON, "SUMMER_FUN");
            purchaseParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggingsCart });

            mFirebase.logEvent(FirebaseAnalytics.Event.PURCHASE, purchaseParams);
        }

        private void refund() {
            Bundle refundParams = new Bundle();
            refundParams.putString(FirebaseAnalytics.Param.TRANSACTION_ID, "T12345");
            refundParams.putString(FirebaseAnalytics.Param.AFFILIATION, "Google Store");
            refundParams.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
            refundParams.putDouble(FirebaseAnalytics.Param.VALUE, 9.99);

            // (Optional) for partial refunds, define the item ID and quantity of refunded items
            refundParams.putString(FirebaseAnalytics.Param.ITEM_ID, "SKU_123");
            refundParams.putLong(FirebaseAnalytics.Param.QUANTITY, 1);

            refundParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggings });

            mFirebase.logEvent(FirebaseAnalytics.Event.REFUND, refundParams);
        }

        private void viewPromotion() {
            Bundle promoParams = new Bundle();
            promoParams.putString(FirebaseAnalytics.Param.PROMOTION_ID, "SUMMER_FUN");
            promoParams.putString(FirebaseAnalytics.Param.PROMOTION_NAME, "Summer Sale");
            promoParams.putString(FirebaseAnalytics.Param.CREATIVE_NAME, "summer2020_promo.jpg");
            promoParams.putString(FirebaseAnalytics.Param.CREATIVE_SLOT, "featured_app_1");
            promoParams.putString(FirebaseAnalytics.Param.LOCATION_ID, "HERO_BANNER");
            promoParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggings });

            mFirebase.logEvent(FirebaseAnalytics.Event.VIEW_PROMOTION, promoParams);
        }

        private void selectPromotion() {
            Bundle promoParams = new Bundle();
            promoParams.putString(FirebaseAnalytics.Param.PROMOTION_ID, "SUMMER_FUN");
            promoParams.putString(FirebaseAnalytics.Param.PROMOTION_NAME, "Summer Sale");
            promoParams.putString(FirebaseAnalytics.Param.CREATIVE_NAME, "summer2020_promo.jpg");
            promoParams.putString(FirebaseAnalytics.Param.CREATIVE_SLOT, "featured_app_1");
            promoParams.putString(FirebaseAnalytics.Param.LOCATION_ID, "HERO_BANNER");
            promoParams.putParcelableArray(FirebaseAnalytics.Param.ITEMS,
                    new Parcelable[]{ itemJeggings });

            mFirebase.logEvent(FirebaseAnalytics.Event.SELECT_PROMOTION, promoParams);
        }

        private void sendCustomEvent() {
            Bundle product = new Bundle();
            product.putString("name", "My Product");
            product.putDouble("price", 10.95);
            mFirebase.logEvent("my_custom_event", product);
        }
    }
}
