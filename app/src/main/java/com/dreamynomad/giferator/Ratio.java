package com.dreamynomad.giferator;

/**
 * Created by Eric on 9/23/2015.
 */
public enum Ratio {
    TWO_ONE("2:1", 2.0f / 1.0f),
    THREE_TWO("3:2", 3.0f / 2.0f),
    FOUR_THREE("4:3", 4.0f / 3.0f),
    FIVE_FOUR("5:4", 5.0f / 4.0f),
    ONE_ONE("1:1", 1.0f / 1.0f),
    FOUR_FIVE("4:5", 4.0f / 5.0f),
    THREE_FOUR("3:4", 3.0f / 4.0f),
    TWO_THREE("2:3", 2.0f / 3.0f),
    ONE_TWO("1:2", 1.0f / 2.0f),;

    private final String mString;
    private final float mRatio;

    Ratio(String string, float ratio) {
        mString = string;
        mRatio = ratio;
    }

    public float getRatio() {
        return mRatio;
    }

    @Override
    public String toString() {
        return mString;
    }
}
