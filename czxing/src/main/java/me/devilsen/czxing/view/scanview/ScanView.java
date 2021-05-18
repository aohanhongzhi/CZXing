package me.devilsen.czxing.view.scanview;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc : 二维码界面使用类
 */
public class ScanView extends BarCoderView implements ScanBoxView.ScanBoxClickListener,
        BarcodeReader.ReadCodeListener {

    /** 混合扫描模式（默认），扫描4次扫码框里的内容，扫描1次以屏幕宽为边长的内容 */
    public static final int SCAN_MODE_MIX = 0;
    /** 只扫描扫码框里的内容 */
    public static final int SCAN_MODE_TINY = 1;
    /** 扫描以屏幕宽为边长的内容 */
    public static final int SCAN_MODE_BIG = 2;

    private static final int DARK_LIST_SIZE = 4;

    private boolean isStop;
    private boolean isDark;
    private int showCounter;
    private final BarcodeReader reader;
    private ScanListener.AnalysisBrightnessListener brightnessListener;

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScanBoxView.setScanBoxClickListener(this);
        reader = BarcodeReader.getInstance();
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        if (isStop) {
            return;
        }
        reader.read(data, left, top, width, height, rowWidth, rowHeight);
        Log.e("save >>> ", "left = " + left + " top= " + top +
                " width=" + width + " height= " + height + " rowWidth=" + rowWidth + " rowHeight=" + rowHeight);

//        SaveImageUtil.saveData(getContext(), data, left, top, width, height, rowWidth);
    }

    /**
     * 设置亮度分析回调
     */
    public void setAnalysisBrightnessListener(ScanListener.AnalysisBrightnessListener brightnessListener) {
        this.brightnessListener = brightnessListener;
    }

    /**
     * 设置扫描格式
     */
    public void setBarcodeFormat(BarcodeFormat... formats) {
        if (formats == null || formats.length == 0) {
            return;
        }
        reader.setBarcodeFormat(formats);
    }

    @Override
    public void startScan() {
        reader.setReadCodeListener(this);
        super.startScan();
        reader.prepareRead();
        isStop = false;
    }

    @Override
    public void stopScan() {
        super.stopScan();
        reader.stopRead();
        isStop = true;
        reader.setReadCodeListener(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onReadCodeResult(CodeResult result) {
        if (result == null) {
            return;
        }
//        showCodeBorder(result);
        BarCodeUtil.d("result : " + result.toString());

        if (!TextUtils.isEmpty(result.getText()) && !isStop) {
            isStop = true;
            reader.stopRead();
            if (mScanListener != null) {
                mScanListener.onScanSuccess(result.getText(), result.getFormat());
            }
        } else if (result.getPoints() != null) {
            tryZoom(result);
        }
    }

    @Override
    public void onFocus() {
        BarCodeUtil.d("not found code too many times , try focus");
        mCamera.onFrozen();
    }

    @Override
    public void onAnalysisBrightness(boolean isDark) {
        BarCodeUtil.d("isDark : " + isDark);

        if (isDark) {
            showCounter++;
            showCounter = Math.min(showCounter, DARK_LIST_SIZE);
        } else {
            showCounter--;
            showCounter = Math.max(showCounter, 0);
        }

        if (this.isDark) {
            if (showCounter <= 2) {
                this.isDark = false;
                mScanBoxView.setDark(false);
            }
        } else {
            if (showCounter >= DARK_LIST_SIZE) {
                this.isDark = true;
                mScanBoxView.setDark(true);
            }
        }

        if (brightnessListener != null) {
            brightnessListener.onAnalysisBrightness(this.isDark);
        }
    }

    @Override
    public void onFlashLightClick() {
        if (mCamera.isFlashLighting()) {
            closeFlashlight();
        } else if (isDark) {
            openFlashlight();
        }
    }

}
