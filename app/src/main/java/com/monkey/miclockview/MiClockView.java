package com.monkey.miclockview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import java.util.Calendar;

/**
 * 仿小米时钟
 */
public class MiClockView extends View {

    /* 画布 */
    private Canvas mCanvas;
    /* 小时文本画笔 */
    private Paint mTextPaint;
    /* 测量小时文本宽高的矩形 */
    private Rect mTextRect;
    /* 小时圆圈画笔 */
    private Paint mCirclePaint;
    /* 小时圆圈线条宽度 */
    private float mCircleStrokeWidth;
    /* 小时圆圈的外接矩形 */
    private RectF mCircleRectF;
    /* 刻度圆弧画笔 */
    private Paint mScaleArcPaint;
    /* 刻度圆弧的外接矩形 */
    private RectF mScaleArcRectF;
    /* 刻度线画笔 */
    private Paint mScaleLinePaint;
    /* 时针画笔 */
    private Paint mHourHandPaint;
    /* 分针画笔 */
    private Paint mMinuteHandPaint;
    /* 秒针画笔 */
    private Paint mSecondHandPaint;
    /* 时针路径 */
    private Path mHourHandPath;
    /* 分针路径 */
    private Path mMinuteHandPath;
    /* 秒针路径 */
    private Path mSecondHandPath;

    /* 亮色，用于分针、秒针、渐变终止色 */
    private int mLightColor;
    /* 暗色，圆弧、刻度线、时针、渐变起始色 */
    private int mDarkColor;
    /* 背景色 */
    private int mBackgroundColor;
    /* 小时文本字体大小 */
    private float mTextSize;
    /* 时钟半径，不包括padding值 */
    private float mRadius;
    /* 刻度线长度 */
    private float mScaleLength;

    /* 时针角度 */
    private float mHourDegree;
    /* 分针角度 */
    private float mMinuteDegree;
    /* 秒针角度 */
    private float mSecondDegree;

    /* 加一个默认的padding值，为了防止用camera旋转时钟时造成四周超出view大小 */
    private float mDefaultPadding;
    private float mPaddingLeft;
    private float mPaddingTop;
    private float mPaddingRight;
    private float mPaddingBottom;

    /* 梯度扫描渐变 */
    private SweepGradient mSweepGradient;
    /* 渐变矩阵，作用在SweepGradient */
    private Matrix mGradientMatrix;
    /* 触摸时作用在Camera的矩阵 */
    private Matrix mCameraMatrix;
    /* 照相机，用于旋转时钟实现3D效果 */
    private Camera mCamera;
    /* camera绕X轴旋转的角度 */
    private float mCameraRotateX;
    /* camera绕Y轴旋转的角度 */
    private float mCameraRotateY;
    /* camera旋转的最大角度 */
    private float mMaxCameraRotate = 10;
    /* 手指松开时时钟晃动的动画 */
    private ValueAnimator mShakeAnimX;
    private ValueAnimator mShakeAnimY;

    public MiClockView(Context context) {
        this(context, null);
    }

    public MiClockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiClockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MiClockView, defStyleAttr, 0);
        mBackgroundColor = ta.getColor(R.styleable.MiClockView_backgroundColor, Color.parseColor("#237EAD"));
        setBackgroundColor(mBackgroundColor);
        mLightColor = ta.getColor(R.styleable.MiClockView_lightColor, Color.parseColor("#ffffff"));
        mDarkColor = ta.getColor(R.styleable.MiClockView_darkColor, Color.parseColor("#80ffffff"));
        mTextSize = ta.getDimension(R.styleable.MiClockView_textSize, DensityUtils.sp2px(context, 14));
        ta.recycle();

        mHourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHourHandPaint.setStyle(Paint.Style.FILL);
        mHourHandPaint.setColor(mDarkColor);

        mMinuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMinuteHandPaint.setStyle(Paint.Style.FILL);
        mMinuteHandPaint.setColor(mLightColor);

        mSecondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondHandPaint.setStyle(Paint.Style.FILL);
        mSecondHandPaint.setColor(mLightColor);

        mScaleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleLinePaint.setStyle(Paint.Style.STROKE);
        mScaleLinePaint.setColor(mBackgroundColor);

        mScaleArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScaleArcPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(mDarkColor);
        mTextPaint.setTextSize(mTextSize);

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCircleStrokeWidth = DensityUtils.dp2px(context, 1);
        mCirclePaint.setStrokeWidth(mCircleStrokeWidth);
        mCirclePaint.setColor(mDarkColor);

        mTextRect = new Rect();
        mCircleRectF = new RectF();
        mScaleArcRectF = new RectF();
        mHourHandPath = new Path();
        mMinuteHandPath = new Path();
        mSecondHandPath = new Path();

        mGradientMatrix = new Matrix();
        mCameraMatrix = new Matrix();
        mCamera = new Camera();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureDimension(widthMeasureSpec), measureDimension(heightMeasureSpec));
    }

    private int measureDimension(int measureSpec) {
        int result;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            result = size;
        } else {
            result = 800;
            if (mode == MeasureSpec.AT_MOST) {
                result = Math.min(result, size);
            }
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //宽和高分别去掉padding值，取min的一半即表盘的半径
        mRadius = Math.min(w - getPaddingLeft() - getPaddingRight(),
                h - getPaddingTop() - getPaddingBottom()) / 2;
        mDefaultPadding = 0.12f * mRadius;//根据比例确定默认padding大小
        mPaddingLeft = mDefaultPadding + w / 2 - mRadius + getPaddingLeft();
        mPaddingTop = mDefaultPadding + h / 2 - mRadius + getPaddingTop();
        mPaddingRight = mPaddingLeft;
        mPaddingBottom = mPaddingTop;
        mScaleLength = 0.12f * mRadius;//根据比例确定刻度线长度
        mScaleArcPaint.setStrokeWidth(mScaleLength);
        mScaleLinePaint.setStrokeWidth(0.012f * mRadius);
        //梯度扫描渐变，以(w/2,h/2)为中心点，两种起止颜色梯度渐变
        //float数组表示，[0,0.75)为起始颜色所占比例，[0.75,1}为起止颜色渐变所占比例
        mSweepGradient = new SweepGradient(w / 2, h / 2,
                new int[]{mDarkColor, mLightColor}, new float[]{0.75f, 1});
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mCanvas = canvas;
        setCameraRotate();
        getTimeDegree();
        drawTimeText();
        drawScaleLine();
        drawSecondHand();
        drawHourHand();
        drawMinuteHand();
        drawCoverCircle();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getCameraRotate(event);
                break;
            case MotionEvent.ACTION_MOVE:
                //根据手指坐标计算camera应该旋转的大小
                getCameraRotate(event);
                break;
            case MotionEvent.ACTION_UP:
                //松开手指，时钟复原并伴随晃动动画
                mShakeAnimX = getShakeAnim(mCameraRotateX, 0);
                mShakeAnimX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mCameraRotateX = (float) valueAnimator.getAnimatedValue();
                    }
                });
                mShakeAnimY = getShakeAnim(mCameraRotateY, 0);
                mShakeAnimY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        mCameraRotateY = (float) valueAnimator.getAnimatedValue();
                    }
                });
                break;
        }
        return true;
    }

    /**
     * 获取camera旋转的大小
     * 注意view坐标与camera坐标方向的转换
     */
    private void getCameraRotate(MotionEvent event) {
        if (mShakeAnimX != null && mShakeAnimX.isRunning()) {
            mShakeAnimX.cancel();
            mShakeAnimY.cancel();
        }
        float rotateX = -(event.getY() - getHeight() / 2);
        float rotateY = (event.getX() - getWidth() / 2);
        //求出此时旋转的大小与半径之比
        float percentX = rotateX / mRadius;
        float percentY = rotateY / mRadius;
        if (percentX > 1) {
            percentX = 1;
        } else if (percentX < -1) {
            percentX = -1;
        }
        if (percentY > 1) {
            percentY = 1;
        } else if (percentY < -1) {
            percentY = -1;
        }
        //最终旋转的大小按比例匀称改变
        mCameraRotateX = percentX * mMaxCameraRotate;
        mCameraRotateY = percentY * mMaxCameraRotate;
    }

    /**
     * 设置3D时钟效果，触摸矩阵的相关设置、照相机的旋转大小
     * 应用在绘制图形之前，否则无效
     */
    private void setCameraRotate() {
        mCameraMatrix.reset();
        mCamera.save();
        mCamera.rotateX(mCameraRotateX);//绕x轴旋转角度
        mCamera.rotateY(mCameraRotateY);//绕y轴旋转角度
        mCamera.getMatrix(mCameraMatrix);//相关属性设置到matrix中
        mCamera.restore();
        //camera在view左上角那个点，故旋转默认是以左上角为中心旋转
        //故在动作之前pre将matrix向左移动getWidth()/2长度，向上移动getHeight()/2长度
        mCameraMatrix.preTranslate(-getWidth() / 2, -getHeight() / 2);
        //在动作之后post再回到原位
        mCameraMatrix.postTranslate(getWidth() / 2, getHeight() / 2);
        mCanvas.concat(mCameraMatrix);//matrix与canvas相关联
    }

    /**
     * 时钟晃动动画
     */
    private ValueAnimator getShakeAnim(float start, float end) {
        ValueAnimator anim = ValueAnimator.ofFloat(start, end);
        anim.setInterpolator(new OvershootInterpolator(10));
        anim.setDuration(500);
        anim.start();
        return anim;
    }

    /**
     * 获取当前时分秒所对应的角度
     * 为了不让秒针走得像老式挂钟一样僵硬，需要精确到毫秒
     */
    private void getTimeDegree() {
        Calendar calendar = Calendar.getInstance();
        float milliSecond = calendar.get(Calendar.MILLISECOND);
        float second = calendar.get(Calendar.SECOND) + milliSecond / 1000;
        float minute = calendar.get(Calendar.MINUTE) + second / 60;
        float hour = calendar.get(Calendar.HOUR) + minute / 60;
        mSecondDegree = second / 60 * 360;
        mMinuteDegree = minute / 60 * 360;
        mHourDegree = hour / 12 * 360;
    }

    /**
     * 画最外圈的时间文本和4个弧线
     */
    private void drawTimeText() {
        String timeText = "12";
        mTextPaint.getTextBounds(timeText, 0, timeText.length(), mTextRect);
        int textLargeWidth = mTextRect.width();//两位数字的宽
        mCanvas.drawText("12", getWidth() / 2 - textLargeWidth / 2, mPaddingTop + mTextRect.height(), mTextPaint);
        timeText = "3";
        mTextPaint.getTextBounds(timeText, 0, timeText.length(), mTextRect);
        int textSmallWidth = mTextRect.width();//一位数字的宽
        mCanvas.drawText("3", getWidth() - mPaddingRight - mTextRect.height() / 2 - textSmallWidth / 2,
                getHeight() / 2 + mTextRect.height() / 2, mTextPaint);
        mCanvas.drawText("6", getWidth() / 2 - textSmallWidth / 2, getHeight() - mPaddingBottom, mTextPaint);
        mCanvas.drawText("9", mPaddingLeft + mTextRect.height() / 2 - textSmallWidth / 2,
                getHeight() / 2 + mTextRect.height() / 2, mTextPaint);

        //画4个弧
        mCircleRectF.set(mPaddingLeft + mTextRect.height() / 2 + mCircleStrokeWidth / 2,
                mPaddingTop + mTextRect.height() / 2 + mCircleStrokeWidth / 2,
                getWidth() - mPaddingRight - mTextRect.height() / 2 + mCircleStrokeWidth / 2,
                getHeight() - mPaddingBottom - mTextRect.height() / 2 + mCircleStrokeWidth / 2);
        for (int i = 0; i < 4; i++) {
            mCanvas.drawArc(mCircleRectF, 5 + 90 * i, 80, false, mCirclePaint);
        }
    }

    /**
     * 画一圈梯度渲染的亮暗色渐变圆弧，重绘时不断旋转，上面盖一圈背景色的刻度线
     */
    private void drawScaleLine() {
        mScaleArcRectF.set(mPaddingLeft + 1.5f * mScaleLength + mTextRect.height() / 2,
                mPaddingTop + 1.5f * mScaleLength + mTextRect.height() / 2,
                getWidth() - mPaddingRight - mTextRect.height() / 2 - 1.5f * mScaleLength,
                getHeight() - mPaddingBottom - mTextRect.height() / 2 - 1.5f * mScaleLength);
        //matrix默认会在三点钟方向开始颜色的渐变，为了吻合钟表十二点钟顺时针旋转的方向，把秒针旋转的角度减去90度
        mGradientMatrix.setRotate(mSecondDegree - 90, getWidth() / 2, getHeight() / 2);
        mSweepGradient.setLocalMatrix(mGradientMatrix);
        mScaleArcPaint.setShader(mSweepGradient);
        mCanvas.drawArc(mScaleArcRectF, 0, 360, false, mScaleArcPaint);
        //画背景色刻度线
        mCanvas.save();
        for (int i = 0; i < 200; i++) {
            mCanvas.drawLine(getWidth() / 2, mPaddingTop + mScaleLength + mTextRect.height() / 2,
                    getWidth() / 2, mPaddingTop + 2 * mScaleLength + mTextRect.height() / 2, mScaleLinePaint);
            mCanvas.rotate(1.8f, getWidth() / 2, getHeight() / 2);
        }
        mCanvas.restore();
    }

    /**
     * 画秒针，根据不断变化的秒针角度旋转画布
     */
    private void drawSecondHand() {
        mCanvas.save();
        mCanvas.rotate(mSecondDegree, getWidth() / 2, getHeight() / 2);
        mSecondHandPath.reset();
        float offset = mPaddingTop + mTextRect.height() / 2;
        mSecondHandPath.moveTo(getWidth() / 2, offset + 0.27f * mRadius);
        mSecondHandPath.lineTo(getWidth() / 2 - 0.05f * mRadius, offset + 0.35f * mRadius);
        mSecondHandPath.lineTo(getWidth() / 2 + 0.05f * mRadius, offset + 0.35f * mRadius);
        mSecondHandPath.close();
        mSecondHandPaint.setColor(mLightColor);
        mCanvas.drawPath(mSecondHandPath, mSecondHandPaint);
        mCanvas.restore();
    }

    /**
     * 画时针，根据不断变化的时针角度旋转画布
     * 针头为圆弧状，使用二阶贝塞尔曲线
     */
    private void drawHourHand() {
        mCanvas.save();
        mCanvas.rotate(mHourDegree, getWidth() / 2, getHeight() / 2);
        mHourHandPath.reset();
        float offset = mPaddingTop + mTextRect.height() / 2;
        mHourHandPath.moveTo(getWidth() / 2 - 0.02f * mRadius, getHeight() / 2);
        mHourHandPath.lineTo(getWidth() / 2 - 0.01f * mRadius, offset + 0.5f * mRadius);
        mHourHandPath.quadTo(getWidth() / 2, offset + 0.48f * mRadius,
                getWidth() / 2 + 0.01f * mRadius, offset + 0.5f * mRadius);
        mHourHandPath.lineTo(getWidth() / 2 + 0.02f * mRadius, getHeight() / 2);
        mHourHandPath.close();
        mCanvas.drawPath(mHourHandPath, mHourHandPaint);
        mCanvas.restore();
    }

    /**
     * 画分针，根据不断变化的分针角度旋转画布
     */
    private void drawMinuteHand() {
        mCanvas.save();
        mCanvas.rotate(mMinuteDegree, getWidth() / 2, getHeight() / 2);
        mMinuteHandPath.reset();
        float offset = mPaddingTop + mTextRect.height() / 2;
        mMinuteHandPath.moveTo(getWidth() / 2 - 0.01f * mRadius, getHeight() / 2);
        mMinuteHandPath.lineTo(getWidth() / 2 - 0.008f * mRadius, offset + 0.38f * mRadius);
        mMinuteHandPath.quadTo(getWidth() / 2, offset + 0.36f * mRadius,
                getWidth() / 2 + 0.008f * mRadius, offset + 0.38f * mRadius);
        mMinuteHandPath.lineTo(getWidth() / 2 + 0.01f * mRadius, getHeight() / 2);
        mMinuteHandPath.close();
        mCanvas.drawPath(mMinuteHandPath, mMinuteHandPaint);
        mCanvas.restore();
    }

    /**
     * 画指针的连接圆圈，盖住指针path在圆心的连接线
     */
    private void drawCoverCircle() {
        mCanvas.drawCircle(getWidth() / 2, getHeight() / 2, 0.05f * mRadius, mSecondHandPaint);
        mSecondHandPaint.setColor(mBackgroundColor);
        mCanvas.drawCircle(getWidth() / 2, getHeight() / 2, 0.025f * mRadius, mSecondHandPaint);
    }
}
