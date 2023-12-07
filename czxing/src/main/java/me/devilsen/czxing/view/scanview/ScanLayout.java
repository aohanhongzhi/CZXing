package me.devilsen.czxing.view.scanview;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import me.devilsen.czxing.R;
import me.devilsen.czxing.code.BarcodeDecoder;
import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.view.PointView;

/**
 * Created by dongSen on 2023/4/3
 */
public class ScanLayout extends FrameLayout implements BarcodeDecoder.OnFocusListener, BarcodeDecoder.OnDetectBrightnessListener, BarcodeDecoder.OnDetectCodeListener, View.OnClickListener {

    private String mFlashLightOnText;
    private String mFlashLightOffText;
    private ScanBoxView mScanBox;
    private TextView mFlashLightNoticeTextView;
    private ImageView mFlashLightImageView;
    private View mMaskView;

    private int mLightOnResource;
    private int mLightOffResource;
    private boolean mDropFlashLight;
    private int mMaskColor;

    private Handler mHandler;
    private final List<View> mResultViews = new CopyOnWriteArrayList<>();
    private int mPointSize;
    private int mResultColor;
    private boolean mIsHideResultColor;

    private DetectView mDetectView;

    private ScanListener mScanListener;
    private ScanListener.AnalysisBrightnessListener mAnalysisBrightnessListener;
    private String mScanNoticeText;
    private boolean mFlashLightIsOpen;
    private int mBrightnessThreshold;

    public ScanLayout(@NonNull Context context) {
        this(context, null);
    }

    public ScanLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_scan_layout, this, true);
        mDetectView = view.findViewById(R.id.detect_view);
        mScanBox = view.findViewById(R.id.scan_box);
        mFlashLightNoticeTextView = view.findViewById(R.id.text_scan_flashlight_notice);
        mFlashLightImageView = view.findViewById(R.id.image_scan_flashlight);
        mMaskView = view.findViewById(R.id.view_scan_mask);

        mFlashLightOnText = getResources().getText(R.string.czxing_click_open_flash_light).toString();
        mFlashLightOffText = getResources().getText(R.string.czxing_click_close_flash_light).toString();
        mLightOnResource = R.drawable.ic_highlight_open_24dp;
        mLightOffResource = R.drawable.ic_highlight_close_24dp;

        mHandler = new Handler(Looper.getMainLooper());
        mPointSize = BarCodeUtil.dp2px(context, 15);

        mDetectView.setOnDetectCodeListener(this);
        mDetectView.setOnDetectBrightnessListener(this);
        mDetectView.setOnFocusListener(this);

        mFlashLightImageView.setOnClickListener(this);
    }

    public void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mScanBox != null) {
            mScanBox.onDestroy();
            mScanBox = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.image_scan_flashlight) {
            if (mFlashLightIsOpen) {
                mDetectView.closeFlashlight();
                mFlashLightNoticeTextView.setText(mFlashLightOnText);
                mFlashLightImageView.setImageResource(mLightOffResource);
                mFlashLightIsOpen = false;
            } else {
                mDetectView.openFlashlight();
                mFlashLightNoticeTextView.setText(mFlashLightOffText);
                mFlashLightImageView.setImageResource(mLightOnResource);
                mFlashLightIsOpen = true;
            }
        }
    }

    @Override
    public void onReadCodeResult(List<CodeResult> resultList) {
        // 扫码结果
        for (CodeResult result : resultList) {
            BarCodeUtil.d("result : 2" + result.toString());
        }

        showResultPoint(resultList);
    }

    @Override
    public void onAnalysisBrightness(double brightness) {
        if (mAnalysisBrightnessListener != null) {
            mAnalysisBrightnessListener.onAnalysisBrightness(brightness);
        }
//        BarCodeUtil.d("brightness = " + brightness);

        if (mFlashLightIsOpen) {
            return;
        }
        if (brightness < 80) {
            if (mBrightnessThreshold < 20) {
                mBrightnessThreshold++;
            }
        } else {
            if (mBrightnessThreshold > 0) {
                mBrightnessThreshold--;
            }
        }

        if (mBrightnessThreshold > 10) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mFlashLightNoticeTextView.setVisibility(VISIBLE);
                    mFlashLightImageView.setVisibility(VISIBLE);
                }
            });


        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mFlashLightNoticeTextView.setVisibility(GONE);
                    mFlashLightImageView.setVisibility(GONE);
                }
            });
        }
    }

    @Override
    public void onFocus() {

    }

    private void showResultPoint(List<CodeResult> resultList) {
        if (mIsHideResultColor) return;

        removeResultViews();

        for (CodeResult result : resultList) {
            addPointView(result, resultList.size());
        }
    }

    private void removeResultViews() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (View view : mResultViews) {
                    removeView(view);
                }
            }
        });
    }

    private void addPointView(final CodeResult result, int resultSize) {
        int[] points = result.getPoints();
        if (points == null || points.length < 4) return;
        int x = points[0];
        int y = points[1];
        int width = points[2];
        int height = points[3];

        final PointView view = new PointView(getContext());
        if (resultSize > 1) {
            view.drawArrow();
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mScanListener != null) {
                        mScanListener.onClickResult(result);
                    }
                }
            });
        }
        mResultViews.add(view);
        if (mResultColor > 0) {
            view.setColor(mResultColor);
        }

        int xOffset = (width - mPointSize) / 2;
        int yOffset = (height - mPointSize) / 2;

        xOffset = Math.max(xOffset, 0);
        yOffset = Math.max(yOffset, 0);

        final LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.leftMargin = x + xOffset;
        params.topMargin = y + yOffset;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                addView(view, params);
            }
        });
    }

    public void openCamera() {
        mDetectView.openCamera();
    }

    public void closeCamera() {
        mDetectView.closeCamera();
    }

    public void startDetect() {
        mDetectView.startDetect();
    }

    public void stopDetect() {
        mDetectView.stopDetect();
    }

    public void setDetectModel(String detectorPrototxtPath, String detectorCaffeModelPath, String superResolutionPrototxtPath, String superResolutionCaffeModelPath) {
        mDetectView.setDetectModel(detectorPrototxtPath, detectorCaffeModelPath, superResolutionPrototxtPath, superResolutionCaffeModelPath);
    }

    /**
     * 设置手电筒打开时的图标
     */
    public void setFlashLightOnDrawable(int lightOnDrawable) {
        if (lightOnDrawable == 0) {
            return;
        }
        mLightOnResource = lightOnDrawable;
    }

    /**
     * 设置手电筒关闭时的图标
     */
    public void setFlashLightOffDrawable(int lightOffDrawable) {
        if (lightOffDrawable == 0) {
            return;
        }
        mLightOffResource = lightOffDrawable;
        mFlashLightImageView.setImageResource(lightOffDrawable);
    }

    /**
     * 不使用手电筒图标及提示
     */
    public void invisibleFlashLightIcon() {
        mDropFlashLight = true;
    }

    /**
     * 设置闪光灯打开时的提示文字
     */
    public void setFlashLightOnText(String lightOnText) {
        if (lightOnText != null) {
            mFlashLightOnText = lightOnText;
        }
    }

    /**
     * 设置闪光灯关闭时的提示文字
     */
    public void setFlashLightOffText(String lightOffText) {
        if (lightOffText != null) {
            mFlashLightOffText = lightOffText;
        }
    }

    /**
     * 设置扫码得到结果后的遮罩颜色
     *
     * @param color 透明颜色
     */
    public void setResultMaskColor(int color) {
        if (color == 0) {
            return;
        }
        mMaskColor = color;
    }

    public void setResultColor(int resultColor) {
        mResultColor = resultColor;
    }

    public void hideResultColor(boolean hideResultColor) {
        mIsHideResultColor = hideResultColor;
    }

    public ScanBoxView getScanBox() {
        return mScanBox;
    }

    public void setOnScanListener(ScanListener listener) {
        this.mScanListener = listener;
    }

    public void setAnalysisBrightnessListener(ScanListener.AnalysisBrightnessListener listener) {
        this.mAnalysisBrightnessListener = listener;
    }

    public void setBarcodeFormat(BarcodeFormat[] barcodeFormat) {
        mDetectView.setBarcodeFormat(barcodeFormat);
    }

    public void setScanNoticeText(String scanNoticeText) {
        mScanNoticeText = scanNoticeText;
    }

    public void resetZoom() {
        mDetectView.resetZoom();
    }

    public void stopPreview() {
        mDetectView.stopPreview();
    }

    public interface ScanBoxClickListener {
        void onFlashLightClick();
    }


}
