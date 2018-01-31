package com.tang.circlemenulayout.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;


import com.tang.circlemenulayout.R;
import com.tang.circlemenulayout.model.ChildBean;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/12/19.
 */

public class CircleMenuLayout extends ViewGroup {
    private static final String TAG = "CircleMenuLayout";
    /*允许可见项的最大角度*/
    private static final double maxAngle = 288;
    /*允许可见项的最小角度*/
    private static final double minAngle = 72;
    /*转盘初始的角度，可见item从90度开始*/
    private final double initialAngle = 90;
    /*当每秒移动角度达到该值时，认为是快速移动*/
    private static final int FLINGABLE_VALUE = 240;

    /*上下文环境*/
    private Context context;
    /*自动滚动的Runnable*/
    private AutoFlingRunnable mFlingRunnable;
    /*是否在自动滚动*/
    private boolean isFling;
    /*检测按下到抬起时旋转的角度*/
    private float mTmpAngle;
    /*检测按下到抬起时使用的时间*/
    private long mDownTime;
    /*设置初始值*/
    private int mFlingableValue = FLINGABLE_VALUE;

    /*整个圆的半径*/
    private int radius;
    /*外圆半径*/
    private int outRadius;
    /*布局的整个padding*/
    private int padding;
    /*内圆高度*/
    private int inHeight;
    /*开始的角度，初始偏移为90度*/
    private double inAngle = initialAngle;
    /*上一次点击坐标的x值*/
    private float lastX;
    /*上一次点击坐标的y值*/
    private float lastY;
    /*是否内圈可以进行位置变换*/
    private boolean circleInMoveable;
    /*外圆开始的角度，初始偏移90度*/
    private double outAngle = initialAngle;
    /*外圈可以滚动*/
    private boolean circleOutMoveable;
    /*是否显示外圈*/
    private boolean showOut;
    /*外圈已经转动的角度*/
    private double OutAngles;
    /*内圈已经转动的角度*/
    private double inAngles;
    /*外圈子布局被重新布局之前的总体转动角度*/
    private double lastOutAngle;
    /*内圈子布局被重新布局之前的总体转动角度*/
    private double lastInAngle;
    /*外圈滚动的时候将要删除数据的子view的标记*/
    private int deleteTag;
    /*外圈滚动将要添加数据的子view的标记*/
    private int addTag;
    /*内圈滚动的时候将要删除数据的子view的标记*/
    private int inDelTag;
    /*内圈滚动将要添加数据的子view的标记*/
    private int inAddTag;

    /*缓存数据的链表*/
    private ArrayList<String> outCache = new ArrayList<>();
    /*缓存数据的结合*/
    private ArrayList<String> outContentCache = new ArrayList<>();
    /*内圈的缓存数据的链表*/
    private ArrayList<String> inCache = new ArrayList<>();
    /*第一级数据*/
    private String[] ppList = {};
    /*第二级数据*/
    private SparseArray<ArrayList<String>> pList = new SparseArray<>();
    /*最外层数据*/
    ArrayList<ChildBean> children = new ArrayList<>();
    /*当前显示的一级数据，表示的是数据集合中的index*/
    private int ppPos;
    /*当前显示的二级数据，表示的是ppPos对应的数据集合中的index*/
    private int pPos;
    /*二级选中的孩子的tag*/
    private int pTag;
    /*二级选中的孩子的内容*/
    private String pData;

    public CircleMenuLayout(Context context) {
        this(context,null);
    }

    public CircleMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CircleMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setUp(context,attrs);
        init();
    }

    /**
     * 初始化
     * @param context
     * @param attrs
     */
    private void setUp(Context context, AttributeSet attrs) {
        this.context = context;
        //初始化属性
        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.circles);
        padding = (int) typedArray.getDimension(R.styleable.circles_circles_padding,20);
        outRadius = (int) typedArray.getDimension(R.styleable.circles_out_item_radius,20);
        inHeight = (int) typedArray.getDimension(R.styleable.circles_in_text_height,20);
        typedArray.recycle();
        //默认不显示外圈
        showOut = false;
    }

    /**
     * 设置布局
     */
    private void init(){
        LayoutInflater mInflater = LayoutInflater.from(getContext());
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        //设置内圈和外圈的初始值，所有view都创建，一共21个child
        for (int i = 0; i < 10; i++) {
            TextView textView = new TextView(context);
            textView.setTextColor(getResources().getColor(R.color.color_999));
            textView.setTextSize(12);//内部文字
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setPadding(8,0,0,0);
            View view = mInflater.inflate(R.layout.view_circle_item, this, false);
            view.setTag(i);
            textView.setTag(i);
            this.addView(textView,lp);
            this.addView(view);//最外圈item
        }
        initData(0,0);
    }

    /**
     * 设置初始布局
     * @param ppPos
     * @param pPos
     */
    private void initData(int ppPos,int pPos){
        //设置圆盘内圈缓存初始值
        if (pList.size() > ppPos) {
            if (pList.get(ppPos).size() > 6) {//只有数据量大于6的时候才会缓存数据
                for (int i = 0; i < (pList.get(ppPos).size() - 6); i++) {
                    inCache.add(pList.get(ppPos).get(i+6));
                }
            }
        }

        //初始化标记
        deleteTag = inDelTag = 0;
        addTag = inAddTag = 6;

        //设置内圈和外圈的值
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getId() == R.id.id_out_item) {//是外圈
            }else if(getChildAt(i).getId() == R.id.id_center_content){//中心点
                if (ppList.length > ppPos) {
                    ((TextView)getChildAt(i).findViewById(R.id.id_center_text)).setText(ppList[ppPos]);
                }else{
                    ((TextView)getChildAt(i).findViewById(R.id.id_center_text)).setText(" ");
                }
            }else{//内圈
                if (((int)getChildAt(i).getTag()) > 5) {
                    ((TextView)getChildAt(i)).setText(" ");
                }else{
                    if (pList.size()>ppPos) {
                        if (pList.get(ppPos).size() > (int) getChildAt(i).getTag()) {//保证数组越界
                            ((TextView)getChildAt(i)).setText(pList.get(ppPos).get((int) getChildAt(i).getTag()));
                        }else{
                            ((TextView)getChildAt(i)).setText(" ");
                        }
                    }else{
                        ((TextView)getChildAt(i)).setText(" ");
                    }
                }
            }
        }
        //设置内圈的默认选中值
        pData = "";
        if (pList.size() > ppPos) {
            if (pList.get(ppPos).size()>1) {
                pData = pList.get(ppPos).get(0);
            }
        }
    }

    /**
     * 给每个子view设置位置，并且给相应的item设置事件,重新布置位置的地方不改变view的tag
     * @param changed
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, final int l, int t, int r, int b) {
        int anglePile = 36;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getId() == R.id.id_center_content) {//中心点的view
                // 找到中心的view，如果存在设置onclick事件
                child.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isFling) {//当前处于旋转状态，不能进行响应点击
                            return;
                        }
                        if (listener!=null) {
                            listener.onCenterClick();
                            updateAllView();
                        }
                    }
                });
                // 设置center item位置
                int cl = radius - child.getMeasuredWidth()/2 + padding;
                child.layout(cl - 14, cl, cl+child.getMeasuredWidth() -14, cl+child.getMeasuredWidth());//强制中心点向左移动14像素
            }else if(child instanceof TextView){//内圆
                if (pTag == (int) child.getTag() && pData.equals(((TextView) child).getText().toString())) {
                    ((TextView) child).setTextColor(ContextCompat.getColor(context,R.color.color_app_blue));
                    ((TextView) child).setTextSize(14);
                }else{
                    ((TextView) child).setTextColor(ContextCompat.getColor(context,R.color.color_666));
                    ((TextView) child).setTextSize(12);
                }
                inAngle = (inAngle%360+360)%360;
                //中心点到item的位置
                int distance = (int) (radius/2.0f);
                //横坐标
                int left = (int) (radius+ Math.round(distance * Math.cos(Math.toRadians(inAngle)) - 1 / 2f * radius/2f)) + padding;
                //纵坐标
                int top = (int) (radius - Math.round(distance * Math.sin(Math.toRadians(inAngle)))) + padding - inHeight/2;

                child.layout(left, top,left + radius/2, top+inHeight );
                child.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isFling) {//当前处于旋转状态，不能响应点击
                            return;
                        }
                        if (listener!=null) {
                            pTag = (int) v.getTag();
                            pData = ((TextView)v).getText().toString();
                            updateOutCircle(pData);
                            listener.onItemInClick(v);
                        }
                    }
                });
                //根据顺时针和逆时针设置数据
                if (lastInAngle <= inAngles) {//逆时针转动
                    if(inAngle <= maxAngle && inAngle >= minAngle){//会有一个新的view突然可见，设置该新的view的内容
                        child.setVisibility(VISIBLE);
                        if(inDelTag == (((int)child.getTag())+1)%10){
                            ((TextView)child).setText(inCache.get(inCache.size()-1));
                            inCache.remove(inCache.size()-1);
                            inDelTag = inDelTag == 0? 9:inDelTag-1;
                        }
                    }else{
                        child.setVisibility(GONE);
                        if (inAddTag == (((int)child.getTag())+1)%10) {
                            inCache.add(0,(String) ((TextView)child).getText());
                            inAddTag = inAddTag == 0?9:inAddTag -1;
                        }
                    }
                }else{//顺时针转动
                    if(inAngle <= maxAngle && inAngle >= minAngle){//会有一个新的view突然可见，设置该新的view的内容
                        child.setVisibility(VISIBLE);
                        if (((int)child.getTag()) == inAddTag) {
                            //添加一个视图
                            ((TextView)child).setText(inCache.get(0));
                            inCache.remove(0);
                            inAddTag = (inAddTag+1)%10;
                        }
                    }else{//会有一个新的视图不可见，保存不可见的视图的内容
                        child.setVisibility(GONE);
                        if (((int)child.getTag()) == inDelTag) {
                            //删除一个视图
                            inCache.add((String) ((TextView)child).getText());
                            inDelTag = (inDelTag + 1)%10;
                        }
                    }
                }
                inAngle += anglePile;
            }else{//-----------------------------------------外圆-------------------------------------------------//
                if (showOut == false) {//不显示外圈
                    child.setVisibility(INVISIBLE);
                    continue;
                }
                outAngle = (outAngle%360+360)%360;//outAngle为负数了
                //外圆到中心点的距离
                int outDistance = radius/4*3+outRadius;
                //横坐标
                int right =(int) (radius+padding+ Math.round(outDistance * Math.cos(Math.toRadians(outAngle))))+outRadius;
                int bottom = (int) (radius+ padding - Math.round(outDistance * Math.sin(Math.toRadians(outAngle))))+outRadius;
                child.layout(right - outRadius*2, bottom - outRadius * 2,right, bottom);
                child.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener!=null) {
                            listener.onItemOutClick(findEntity(v));
                        }
                    }
                });
                child.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

//                        if (listener!=null) {
//                            listener.onItemOutLongClick(findEntity(v));
//                        }
                        return false;
                    }
                });
                //根据顺时针和逆时针设置数据
                if(lastOutAngle <= OutAngles){//逆时针转动
                    if (outAngle <= maxAngle && outAngle >= minAngle) {//会有一个新的view突然可见，设置该新的view的内容
                        if(deleteTag == (((int)child.getTag())+1)%10){
                            ((TextView)child.findViewById(R.id.out_item_title)).setText(outCache.get(outCache.size()-1));
                            outCache.remove(outCache.size()-1);
                            ((TextView)child.findViewById(R.id.out_item_content)).setText(outContentCache.get(outContentCache.size()-1));
                            outContentCache.remove(outContentCache.size()-1);
                            deleteTag = deleteTag == 0? 9:deleteTag-1;
                        }
                        if (TextUtils.isEmpty(((TextView)child.findViewById(R.id.out_item_content)).getText().toString()
                                + ((TextView)child.findViewById(R.id.out_item_title)).getText().toString())) {
                            child.setVisibility(GONE);
                        }else{
                            child.setVisibility(VISIBLE);
                        }
                    }else {//会有一个新的视图不可见，保存不可见的视图的内容
                        child.setVisibility(INVISIBLE);
                        if (addTag == (((int)child.getTag())+1)%10) {
                            outCache.add(0,(String) ((TextView)child.findViewById(R.id.out_item_title)).getText());
                            outContentCache.add(0,(String) ((TextView)child.findViewById(R.id.out_item_content)).getText());
                            addTag = addTag == 0?9:addTag -1;
                        }
                    }
                }else{//顺时针转动
                    if (outAngle <= maxAngle && outAngle >= minAngle) {//会有一个新的view突然可见，设置该新的view的内容
                        if (((int)child.getTag()) == addTag) {
                            //添加一个视图
                            ((TextView)child.findViewById(R.id.out_item_title)).setText(outCache.get(0));
                            outCache.remove(0);
                            ((TextView)child.findViewById(R.id.out_item_content)).setText(outContentCache.get(0));
                            outContentCache.remove(0);
                            addTag = (addTag+1)%10;
                        }
                        if (TextUtils.isEmpty(((TextView)child.findViewById(R.id.out_item_content)).getText().toString()
                                + ((TextView)child.findViewById(R.id.out_item_title)).getText().toString())) {
                            child.setVisibility(GONE);
                        }else{
                            child.setVisibility(VISIBLE);
                        }
                    }else{//会有一个新的视图不可见，保存不可见的视图的内容
                        child.setVisibility(INVISIBLE);
                        if (((int)child.getTag()) == deleteTag) {
                            //删除一个视图
                            outCache.add((String) ((TextView)child.findViewById(R.id.out_item_title)).getText());
                            outContentCache.add((String) ((TextView)child.findViewById(R.id.out_item_content)).getText());
                            deleteTag = (deleteTag + 1)%10;
                        }
                    }
                }
                outAngle += anglePile;
            }
        }
        lastOutAngle = OutAngles;
        lastInAngle = inAngles;
    }

    /**
     * 获取点击外圈的内容实体
     * @param v
     * @return
     */
    private ChildBean findEntity(View v){
        ChildBean childBean = null;
        TextView title = (TextView) v.findViewById(R.id.out_item_title);
        TextView content = (TextView) v.findViewById(R.id.out_item_content);
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getContent().equals(content.getText().toString())
                    && children.get(i).getTitle().equals(title.getText().toString())) {
                childBean = children.get(i);
            }
        }
        return childBean;
    }

    /**
     * 更新外圈的数据，由于tag和数据的position是不匹配的，因此只有通过字符串来匹配，找到内圈点击的是哪一项
     */
    private void updateOutCircle(String posString) {
        for (int i = 0; i < pList.get(ppPos).size(); i++) {
            if (pList.get(ppPos).get(i).equals(posString)) {//找到对应项
                pPos = i;
            }
        }
        refreshOut();
        //设置动画
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getId() == R.id.id_out_item) {//是外圈
                if (TextUtils.isEmpty(((TextView)getChildAt(i).findViewById(R.id.out_item_content)).getText().toString()
                        + ((TextView)getChildAt(i).findViewById(R.id.out_item_title)).getText().toString())) {
                    continue;
                }
                if (((int)getChildAt(i).getTag()) < 6) {
                    getChildAt(i).clearAnimation();
                    moveAndScale(getChildAt(i)
                            ,(float) (radius* Math.sin(Math.toRadians(36*(int)getChildAt(i).getTag())))
                            ,(float) (radius* Math.cos(Math.toRadians(36*(int)getChildAt(i).getTag()))));
                }
            }
        }

    }

    /**
     * 刷新外圈的显示数据设置新的缓存
     */
    public void refreshOut(){
        showOut = true;
        //初始化所有外圈的数据
        //初始化标记
        deleteTag = 0;
        addTag = 6;
        outCache.clear();
        outContentCache.clear();
        lastOutAngle = 0;
        OutAngles = 0;
        outAngle = 90;
        //设置当前外圈缓存数据
        ArrayList<String> outTitle = new ArrayList<>();
        outTitle = getOutTitle();
        ArrayList<String> outContent = new ArrayList<>();
        outContent = getOutContent();
        for(int i = 0;i< outContent.size() - 6;i++){
            outContentCache.add(outContent.get(i+6));
        }
        for(int i = 0;i< outTitle.size() - 6;i++){
            outCache.add(outTitle.get(i+6));
        }
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getId() == R.id.id_out_item) {//是外圈
                if (((int) getChildAt(i).getTag()) > 5) {
                    ((TextView) getChildAt(i).findViewById(R.id.out_item_content)).setText("");
                    ((TextView) getChildAt(i).findViewById(R.id.out_item_title)).setText("");
                } else {
                    if (outContent.size() > (int) getChildAt(i).getTag()) {
                        ((TextView) getChildAt(i).findViewById(R.id.out_item_content)).setText(outContent.get((int) getChildAt(i).getTag()));
                    } else {
                        ((TextView) getChildAt(i).findViewById(R.id.out_item_content)).setText("");
                    }
                    if (outTitle.size() > (int) getChildAt(i).getTag()) {
                        ((TextView) getChildAt(i).findViewById(R.id.out_item_title)).setText(outTitle.get((int) getChildAt(i).getTag()));
                    } else {
                        ((TextView) getChildAt(i).findViewById(R.id.out_item_title)).setText("");
                    }
                }
            }
        }
        requestLayout();
    }

    /**
     * 刷新整个视图的所有数据,包括中心点，内圆和外圆
     */
    private void updateAllView() {
        if (ppPos < ppList.length - 1) {
            //中心点的数据集合index自增
            ppPos++;
        }else{
            //设置显示默认的第一条数据
            ppPos = 0;
        }
        showOut = false;//默认不显示外圈
        pPos = 0;//设置内圈显示默认的第一条数据
        //将所有的内圈信息初始化
        inCache.clear();
        lastOutAngle = 0;
        inAngles = 0;
        inAngle = 90;
        initData(ppPos,pPos);
        requestLayout();
    }

    /**
     * 给该布局设置宽高，并且给相应的子view设置宽高
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //设置整个布局的宽高
        int circleWidth;
        int circleHeight;
        //分别获取测量模式和测量值
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //指定具体宽高或者match_parent
        if (widthMode != MeasureSpec.EXACTLY
                || heightMode != MeasureSpec.EXACTLY)
        {
            circleWidth = getSuggestedMinimumWidth();
            circleWidth = circleWidth == 0 ? getDefaultWidth() : circleWidth;

            circleHeight = getSuggestedMinimumHeight();
            circleHeight = circleHeight == 0 ? getDefaultWidth() : circleHeight;
        } else
        {
            // 如果都设置为精确值，则直接取小值；
            circleWidth = circleHeight = Math.min(width, height);
        }
        //设置圆的半径
        radius = Math.max(getMeasuredWidth(), getMeasuredHeight())/2;
        radius = radius - padding;
        setMeasuredDimension(circleWidth, circleHeight);

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            // 计算menu item的尺寸；以及和设置好的模式，去对item进行测量
            int makeMeasureSpec = -1;
            if (child.getId() == R.id.id_center_content) {//中心点测绘
                makeMeasureSpec = MeasureSpec.makeMeasureSpec(
                        (int) (circleWidth/8.0f),
                        MeasureSpec.EXACTLY);
                child.measure(makeMeasureSpec,makeMeasureSpec);

            }else if(child instanceof TextView){//内圆测绘
                int heightSpec = -1;
                makeMeasureSpec = MeasureSpec.makeMeasureSpec(radius/2,
                        MeasureSpec.EXACTLY);
                heightSpec = MeasureSpec.makeMeasureSpec(inHeight,
                        MeasureSpec.EXACTLY);
                child.measure(makeMeasureSpec,heightSpec);

            }else{//外圆
                makeMeasureSpec = MeasureSpec.makeMeasureSpec(outRadius * 2,
                        MeasureSpec.EXACTLY);
                child.measure(makeMeasureSpec,makeMeasureSpec);

            }
        }
    }

    /**
     * 点击在圆盘内圆才能滚动
     * dispatchTouchEvent中的down事件和第一次move事件一定会收到，但是如果点击的点没有存在子view，那么该事件就跳过dispatchTouchEvent方法了
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                mDownTime = System.currentTimeMillis();
                mTmpAngle = 0;
                // 如果当前已经在快速滚动
                if (isFling)
                {
                    // 移除快速滚动的回调
                    removeCallbacks(mFlingRunnable);
                    isFling = false;
                }
                if (!isInCenter(x,y)&&!isCircleIn(x,y)) {
                    if (!showOut) {
                        if (listener!=null) {
                            listener.onDisMiss();
                        }
                    }else{
                        if (!isCircleOut(x,y)) {
                            if (listener!=null) {
                                listener.onDisMiss();
                            }
                        }
                    }
                }

                if (isCircleIn(x,y)) {
                    circleInMoveable = true;
                }else{
                    circleInMoveable = false;
                }
                if (isCircleOut(x,y)) {
                    circleOutMoveable = true;
                }else{
                    circleOutMoveable = false;
                }
            case MotionEvent.ACTION_MOVE:
                if (!circleInMoveable&&!circleOutMoveable) {
                    break;
                }
                /**
                 * 获得开始的角度
                 */
                float start = getAngle(lastX, lastY);
                /**
                 * 获得当前的角度
                 */
                float end = getAngle(x, y);

                // 如果是一、四象限，则直接end-start，角度值都是正值
                if (getQuadrant(x, y) == 1 || getQuadrant(x, y) == 4)
                {
                    if (circleInMoveable) {
                        inAngles += start - end;
                        if (inAngles >= 0 ) {
                            inAngles = 0;
                            inAngle = 0+90;
                        }else if(inAngles <=-36*inCache.size()){
                            inAngles = -36*inCache.size();
                            inAngle = -36*inCache.size()+90;
                        }else{
                            inAngle += start - end;
                        }
                    }else{
                        OutAngles += start - end;
                        if (OutAngles >= 0 ) {
                            OutAngles = 0;
                            outAngle = 0+90;
                        }else if(OutAngles <=-36*outCache.size()){
                            OutAngles = -36*outCache.size();
                            outAngle = -36*outCache.size()+90;
                        }else{
                            outAngle += start - end;
                        }
                    }
                    mTmpAngle += start - end;
                } else
                // 二、三象限，色角度值是负值
                {
                    if (circleInMoveable) {
                        inAngles += end - start;
                        if (inAngles >= 0 ) {
                            inAngles = 0;
                            inAngle = 0+90;
                        }else if(inAngles <=-36*inCache.size()){
                            inAngles = -36*inCache.size();
                            inAngle = -36*inCache.size()+90;
                        }else{
                            inAngle += end - start;
                        }
                    }else{
                        OutAngles += end - start;
                        if (OutAngles >= 0 ) {
                            OutAngles = 0;
                            outAngle = 0+90;
                        }else if(OutAngles <=-36*outCache.size()){
                            OutAngles = -36*outCache.size();
                            outAngle = -36*outCache.size()+90;
                        }else{
                            outAngle += end - start;
                        }
                    }
                    mTmpAngle += end - start;
                }
                // 重新布局
                requestLayout();
                lastX = x;
                lastY = y;
                break;
            case MotionEvent.ACTION_UP:
                if (!circleInMoveable && !circleOutMoveable) {
                    break;
                }
                // 计算，每秒移动的角度
                float anglePerSecond = mTmpAngle * 1000
                        / (System.currentTimeMillis() - mDownTime);
                // 如果达到该值认为是快速移动
                if (Math.abs(anglePerSecond) > mFlingableValue && !isFling)
                {
                    // post一个任务，去自动滚动
                    post(mFlingRunnable = new AutoFlingRunnable(anglePerSecond));
                    return true;
                }
                //如果大于一定值就屏蔽点击事件
                if (Math.abs(mTmpAngle) > 3)
                {
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint=new Paint();
        paint.setAntiAlias(true);          //抗锯齿
        paint.setColor(ContextCompat.getColor(context, R.color.color_f6f6f6));//画笔颜色
        paint.setStyle(Paint.Style.FILL);  //画笔风格
        paint.setStrokeWidth(4);           //画笔粗细
        paint.setAlpha(250);
        canvas.drawCircle(radius + padding,radius + padding,radius/4*3,paint);

        if (showOut) {
            paint.setAlpha(240);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(radius/8);
            canvas.drawCircle(radius + padding,radius + padding,radius/8*7,paint);//radius/8*7是起始位置
        }
    }

    /**
     * 自动滚动的任务
     */
    private class AutoFlingRunnable implements Runnable {

        private float angelPerSecond;

        public AutoFlingRunnable(float velocity)
        {
            this.angelPerSecond = velocity;
        }

        public void run()
        {
            // 如果小于20,则停止
            if ((int) Math.abs(angelPerSecond) < 20)
            {
                isFling = false;
                return;
            }
            isFling = true;
            // 不断改变mStartAngle，让其滚动，/30为了避免滚动太快
            if (circleInMoveable) {
                inAngles += angelPerSecond / 30;
                if(inAngles<=-36*inCache.size()){
                    inAngles = -36*inCache.size();
                    inAngle = -36*inCache.size()+90;
                    angelPerSecond = 19;
                }else if(inAngles > 0){
                    inAngles = 0;
                    inAngle = 0+90;
                    angelPerSecond = 19;
                }else{
                    inAngle += (angelPerSecond / 30);
                }
            }else{
                OutAngles += angelPerSecond / 30;
                if(OutAngles<=-36*outCache.size()){
                    OutAngles = -36*outCache.size();
                    outAngle = -36*outCache.size()+90;
                    angelPerSecond = 19;
                }else if(OutAngles > 0){
                    OutAngles = 0;
                    outAngle = 0+90;
                    angelPerSecond = 19;
                }else{
                    outAngle += (angelPerSecond / 30);
                }
            }
            // 逐渐减小这个值
            angelPerSecond /= 1.0666F;
            postDelayed(this, 30);
            // 重新布局
            requestLayout();
        }
    }

    /**
     * 根据触摸的位置，计算角度
     * @param xTouch
     * @param yTouch
     * @return
     */
    private float getAngle(float xTouch, float yTouch) {
        double x = xTouch - (radius + padding);
        double y = yTouch - (radius + padding);
        return (float) (Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
    }

    /**
     * 根据当前位置计算象限
     * @param x
     * @param y
     * @return
     */
    private int getQuadrant(float x, float y) {
        int tmpX = (int) (x - radius - padding );
        int tmpY = (int) (y - radius - padding);
        if (tmpX >= 0)
        {
            return tmpY >= 0 ? 4 : 1;
        } else
        {
            return tmpY >= 0 ? 3 : 2;
        }
    }

    /**
     * 在点中心
     * @return
     */
    private boolean isInCenter(float x,float y){
        if (((x - radius-padding) * (x - radius - padding) + (y - radius - padding) * (y - radius - padding) <= radius*radius/16)){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 判断是否在内圈
     * @param x
     * @param y
     * @return
     */
    private boolean isCircleIn(float x,float y){
        if (((x - radius - padding) * (x - radius - padding) + (y - radius - padding) * (y - radius - padding) > radius*radius/16)
                && ((x - radius - padding) * (x - radius - padding) + (y - radius - padding) * (y - radius - padding) < radius*radius/16*9)) {
            return true;
        }else{
            return false;
        }
    }

    /**
     * 判断是否在外圈的点击区域上
     * @param x
     * @param y
     * @return
     */
    private boolean isCircleOut(float x,float y){
        if (((x - radius - padding) * (x - radius - padding) + (y - radius - padding) * (y - radius - padding) > radius*radius/16*9)
                && ((x - radius - padding) * (x - radius - padding) + (y - radius - padding) * (y - radius - padding) <
                (radius*7/8 + outRadius) * (radius*7/8 + outRadius))) {
            return true;
        }else{
            return false;
        }
    }

    /**
     * 获得默认该layout的尺寸
     * @return
     */
    private int getDefaultWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
    }

    /**
     * 最外层动画出现时的动画
     * @param target
     * @param x
     * @param y
     */
    private void moveAndScale(View target, float x, float y){
        ObjectAnimator moveX = ObjectAnimator.ofFloat(target,"TranslationX",x,0);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(target,"TranslationY",y,0);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target,"scaleX",0,1);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target,"scaleY",0,1);
        AnimatorSet moveAndScale = new AnimatorSet();
        moveAndScale.playTogether(moveX,moveY,scaleX,scaleY);
        moveAndScale.setDuration(500);
        moveAndScale.setInterpolator(new BounceInterpolator());
        moveAndScale.start();
    }

    /**
     * 设置圆盘的数据，每次设置都要刷新圆盘
     * @param ppList
     * @param pList
     */
    public void setData(String[]ppList, SparseArray<ArrayList<String>> pList, ArrayList<ChildBean> children){
        this.ppList = ppList;
        this.pList = pList;
        this.children = children;
        initData(0,0);
        requestLayout();
    }

    /**
     * 获取外圈的数据集合
     * @return
     */
    private ArrayList<String> getOutTitle(){
        ArrayList<String> titles = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getPpId() == ppPos && children.get(i).getpId() == pPos) {
                titles.add(children.get(i).getTitle());
            }
        }
        return titles;
    }

    /**
     * 获取外圈显示的内容
     * @return
     */
    private ArrayList<String> getOutContent(){
        ArrayList<String> contents = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).getPpId() == ppPos && children.get(i).getpId() == pPos) {
                contents.add(children.get(i).getContent());
            }
        }
        return contents;
    }
    /**
     * 回调监听
     */
    public interface OnCircleClickListener{
        void onCenterClick();
        void onItemInClick(View view);
        void onItemOutClick(ChildBean childBean);
        void onItemOutLongClick(ChildBean childBean);
        void onDisMiss();
    }
    public void setOnCircleClickListener(OnCircleClickListener listener){
        this.listener = listener;
    }
    private OnCircleClickListener listener;
}
