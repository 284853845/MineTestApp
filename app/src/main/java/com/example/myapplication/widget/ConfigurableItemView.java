package com.example.myapplication.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.myapplication.R;

/**
 * A reusable two-column item. The right column can be hidden, text, or an input field.
 * The host controls this view's height through its layout parameters.
 */
public class ConfigurableItemView extends LinearLayout {

    public static final int RIGHT_MODE_HIDDEN = 0;
    public static final int RIGHT_MODE_TEXT = 1;
    public static final int RIGHT_MODE_INPUT = 2;
    public static final int OVERFLOW_ELLIPSIS = 0;
    public static final int OVERFLOW_WRAP = 1;

    private static final int LEFT_WIDTH_DP = 100;
    private final AppCompatTextView leftTextView;
    private final AppCompatTextView rightTextView;
    private final AppCompatEditText inputView;
    private final int leftWidthPx;
    private int rightMode = RIGHT_MODE_HIDDEN;
    private boolean inputEditable = true;
    private int contentSpacingPx;
    private android.text.method.KeyListener editableKeyListener;
    private OnClickListener itemClickListener;

    public ConfigurableItemView(Context context) {
        this(context, null);
    }

    public ConfigurableItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConfigurableItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(false);
        setFocusable(false);

        leftWidthPx = dp(LEFT_WIDTH_DP);
        leftTextView = createTextView();
        rightTextView = createTextView();
        rightTextView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        inputView = new AppCompatEditText(context);
        inputView.setSingleLine(true);
        inputView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        inputView.setInputType(InputType.TYPE_CLASS_TEXT);
        inputView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        inputView.setPadding(0, 0, 0, 0);
        inputView.setBackground(null);
        inputView.setId(R.id.configurable_item_input);
        editableKeyListener = inputView.getKeyListener();

        leftTextView.setId(R.id.configurable_item_left);
        rightTextView.setId(R.id.configurable_item_right);
        addView(leftTextView, new LayoutParams(
                leftWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentSpacingPx = dp(12);
        LayoutParams valueParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        valueParams.leftMargin = contentSpacingPx;
        addView(rightTextView, valueParams);
        addView(inputView, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int primaryColor = ContextCompat.getColor(context, R.color.item_text_primary);
        int secondaryColor = ContextCompat.getColor(context, R.color.item_text_secondary);
        float defaultTextSize = sp(15);
        leftTextView.setTextColor(primaryColor);
        rightTextView.setTextColor(secondaryColor);
        inputView.setTextColor(secondaryColor);
        inputView.setHintTextColor(ContextCompat.getColor(context, R.color.item_text_hint));
        leftTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize);
        rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize);
        inputView.setTextSize(TypedValue.COMPLEX_UNIT_PX, defaultTextSize);
        setLeftOverflow(OVERFLOW_ELLIPSIS);
        setRightOverflow(OVERFLOW_WRAP);
        setInputEditable(true);

        if (attrs != null) {
            readAttributes(context, attrs, primaryColor, secondaryColor, defaultTextSize);
        }
        applyMode();
    }

    private AppCompatTextView createTextView() {
        AppCompatTextView view = new AppCompatTextView(getContext());
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(true);
        view.setPadding(0, 0, 0, 0);
        return view;
    }

    private void readAttributes(Context context, AttributeSet attrs, int primaryColor,
                                int secondaryColor, float defaultTextSize) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ConfigurableItemView);
        setLeftText(a.getString(R.styleable.ConfigurableItemView_item_leftText));
        setRightText(a.getString(R.styleable.ConfigurableItemView_item_rightText));
        setInputText(a.getString(R.styleable.ConfigurableItemView_item_inputText));
        setInputHint(a.getString(R.styleable.ConfigurableItemView_item_inputHint));
        setRightMode(a.getInt(R.styleable.ConfigurableItemView_item_rightMode, RIGHT_MODE_HIDDEN));
        setInputEditable(a.getBoolean(R.styleable.ConfigurableItemView_item_inputEditable, true));
        setLeftOverflow(a.getInt(R.styleable.ConfigurableItemView_item_leftOverflow, OVERFLOW_ELLIPSIS));
        setRightOverflow(a.getInt(R.styleable.ConfigurableItemView_item_rightOverflow, OVERFLOW_WRAP));
        if (a.hasValue(R.styleable.ConfigurableItemView_item_rightIcon)) {
            setRightIcon(a.getDrawable(R.styleable.ConfigurableItemView_item_rightIcon));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_leftTextSize)) {
            setLeftTextSizePx(a.getDimension(R.styleable.ConfigurableItemView_item_leftTextSize, defaultTextSize));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_rightTextSize)) {
            setRightTextSizePx(a.getDimension(R.styleable.ConfigurableItemView_item_rightTextSize, defaultTextSize));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_inputTextSize)) {
            inputView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    a.getDimension(R.styleable.ConfigurableItemView_item_inputTextSize, defaultTextSize));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_leftTextColor)) {
            setLeftTextColor(a.getColor(R.styleable.ConfigurableItemView_item_leftTextColor, primaryColor));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_rightTextColor)) {
            setRightTextColor(a.getColor(R.styleable.ConfigurableItemView_item_rightTextColor, secondaryColor));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_inputTextColor)) {
            setInputTextColor(a.getColor(R.styleable.ConfigurableItemView_item_inputTextColor, secondaryColor));
        }
        if (a.hasValue(R.styleable.ConfigurableItemView_item_inputHintColor)) {
            setInputHintTextColor(a.getColor(
                    R.styleable.ConfigurableItemView_item_inputHintColor, secondaryColor));
        }
        int spacing = a.getDimensionPixelSize(R.styleable.ConfigurableItemView_item_contentSpacing, dp(12));
        setContentSpacingPx(spacing);
        int iconPadding = a.getDimensionPixelSize(R.styleable.ConfigurableItemView_item_rightIconPadding, dp(6));
        setRightIconPaddingPx(iconPadding);
        a.recycle();
    }

    public void setLeftText(@Nullable CharSequence text) {
        leftTextView.setText(text);
    }

    public void setRightText(@Nullable CharSequence text) {
        rightTextView.setText(text);
    }

    public void setInputText(@Nullable CharSequence text) {
        inputView.setText(text);
    }

    public CharSequence getInputText() {
        return inputView.getText();
    }

    public void setInputHint(@Nullable CharSequence hint) {
        inputView.setHint(hint);
    }

    public void setRightMode(int mode) {
        if (mode < RIGHT_MODE_HIDDEN || mode > RIGHT_MODE_INPUT) {
            mode = RIGHT_MODE_HIDDEN;
        }
        rightMode = mode;
        applyMode();
    }

    public int getRightMode() {
        return rightMode;
    }

    public void setRightIcon(@Nullable Drawable drawable) {
        rightTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null);
    }

    public void setRightIcon(@DrawableRes int drawableRes) {
        setRightIcon(drawableRes == 0 ? null : ContextCompat.getDrawable(getContext(), drawableRes));
    }

    public void setLeftOverflow(int overflow) {
        configureOverflow(leftTextView, overflow, OVERFLOW_ELLIPSIS);
    }

    public void setRightOverflow(int overflow) {
        configureOverflow(rightTextView, overflow, OVERFLOW_WRAP);
    }

    public void setInputEditable(boolean editable) {
        inputEditable = editable;
        inputView.setFocusable(editable);
        inputView.setFocusableInTouchMode(editable);
        inputView.setCursorVisible(editable);
        inputView.setLongClickable(editable);
        inputView.setClickable(editable || itemClickListener != null);
        inputView.setKeyListener(editable ? editableKeyListener : null);
        inputView.setOnKeyListener(editable ? null : new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return true;
            }
        });
    }

    public boolean isInputEditable() {
        return inputEditable;
    }

    public void setLeftTextSize(float sizeSp) {
        leftTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    public void setRightTextSize(float sizeSp) {
        rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    public void setInputTextSize(float sizeSp) {
        inputView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
    }

    public void setLeftTextColor(int color) {
        leftTextView.setTextColor(color);
    }

    public void setRightTextColor(int color) {
        rightTextView.setTextColor(color);
    }

    public void setInputTextColor(int color) {
        inputView.setTextColor(color);
    }

    public void setInputHintTextColor(int color) {
        inputView.setHintTextColor(color);
    }

    public void setRightIconPaddingPx(int paddingPx) {
        rightTextView.setCompoundDrawablePadding(Math.max(0, paddingPx));
    }

    public TextView getLeftTextView() {
        return leftTextView;
    }

    public TextView getRightTextView() {
        return rightTextView;
    }

    public AppCompatEditText getInputView() {
        return inputView;
    }

    /** Alias with a domain-specific name for callers that want an item click listener. */
    public void setOnItemClickListener(@Nullable OnClickListener listener) {
        itemClickListener = listener;
        setOnClickListener(listener);
        setClickable(listener != null);
        setFocusable(listener != null);
        inputView.setClickable(inputEditable || listener != null);
        inputView.setOnClickListener(listener == null ? null : new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemClickListener != null) {
                    itemClickListener.onClick(ConfigurableItemView.this);
                }
            }
        });
    }

    public int getLeftWidthPx() {
        return leftWidthPx;
    }

    private void applyMode() {
        rightTextView.setVisibility(rightMode == RIGHT_MODE_TEXT ? VISIBLE : GONE);
        inputView.setVisibility(rightMode == RIGHT_MODE_INPUT ? VISIBLE : GONE);
        LayoutParams params = (LayoutParams) rightTextView.getLayoutParams();
        if (params != null) {
            params.leftMargin = rightMode == RIGHT_MODE_HIDDEN ? 0 : contentSpacingPx;
            rightTextView.setLayoutParams(params);
        }
        LayoutParams inputParams = (LayoutParams) inputView.getLayoutParams();
        if (inputParams != null) {
            inputParams.leftMargin = rightMode == RIGHT_MODE_HIDDEN ? 0 : contentSpacingPx;
            inputView.setLayoutParams(inputParams);
        }
    }

    private void configureOverflow(AppCompatTextView view, int overflow, int defaultValue) {
        int resolved = overflow == OVERFLOW_ELLIPSIS || overflow == OVERFLOW_WRAP
                ? overflow : defaultValue;
        if (resolved == OVERFLOW_WRAP) {
            view.setSingleLine(false);
            view.setMaxLines(Integer.MAX_VALUE);
            view.setEllipsize(null);
            view.setHorizontallyScrolling(false);
        } else {
            view.setSingleLine(true);
            view.setMaxLines(1);
            view.setEllipsize(TextUtils.TruncateAt.END);
        }
    }

    private void setLeftTextSizePx(float px) {
        leftTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
    }

    private void setRightTextSizePx(float px) {
        rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
    }

    public void setContentSpacingPx(int spacingPx) {
        contentSpacingPx = Math.max(0, spacingPx);
        LayoutParams rightParams = (LayoutParams) rightTextView.getLayoutParams();
        LayoutParams inputParams = (LayoutParams) inputView.getLayoutParams();
        if (rightParams != null) {
            rightParams.leftMargin = rightMode == RIGHT_MODE_HIDDEN ? 0 : contentSpacingPx;
            rightTextView.setLayoutParams(rightParams);
        }
        if (inputParams != null) {
            inputParams.leftMargin = rightMode == RIGHT_MODE_HIDDEN ? 0 : contentSpacingPx;
            inputView.setLayoutParams(inputParams);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float sp(int value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.leftText = leftTextView.getText();
        state.rightText = rightTextView.getText();
        state.inputText = inputView.getText();
        state.rightMode = rightMode;
        state.inputEditable = inputEditable;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setLeftText(savedState.leftText);
        setRightText(savedState.rightText);
        setInputText(savedState.inputText);
        setRightMode(savedState.rightMode);
        setInputEditable(savedState.inputEditable);
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    private static class SavedState extends BaseSavedState {
        CharSequence leftText;
        CharSequence rightText;
        CharSequence inputText;
        int rightMode;
        boolean inputEditable;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel source) {
            super(source);
            leftText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            rightText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            inputText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            rightMode = source.readInt();
            inputEditable = source.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            TextUtils.writeToParcel(leftText, out, flags);
            TextUtils.writeToParcel(rightText, out, flags);
            TextUtils.writeToParcel(inputText, out, flags);
            out.writeInt(rightMode);
            out.writeInt(inputEditable ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
