package com.example.daily.source;

import android.app.Activity;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.daily.R;

public class FloatDragView {

    private Activity context; // 上下文
    private ImageView mImageView; // 可拖动按钮
    private static int mScreenWidth = -1; //屏幕的宽度
    private static int mScreenHeight = -1; //屏幕的高度
    private int relativeMoveX; // 控件相对屏幕左上角移动的位置X
    private int relativeMoveY; // 控件相对屏幕左上角移动的位置Y
    private boolean isIntercept = false; // 是否截断touch事件
    private int startDownX; // 按下时的位置控件相对屏幕左上角的位置X
    private int startDownY; // 按下时的位置控件距离屏幕左上角的位置Y
    private static int[] lastPosition; // 用于记录上一次的位置(坐标0对应x,坐标1对应y)


    /**
     * @param context 上下文
     * @param mViewContainer 可拖动按钮要存放的对应的Layout
     * @param clickListener 可拖动按钮的点击事件
     */
    public static ImageView addFloatDragView(Activity context , RelativeLayout mViewContainer,
                                             View.OnClickListener clickListener) {
        FloatDragView floatDragView = new FloatDragView(context);
        ImageView imageView = floatDragView.getFloatDragView(clickListener);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                200,200
        );
        imageView.setLayoutParams(layoutParams);

        mViewContainer.addView(imageView);
        return imageView;
    }

    // 初始化实例
    private FloatDragView(Activity context) {
        setScreenHW(context);
        this.context = context;
        lastPosition = new int[]{0,0};
    }

    // 获取可拖动按钮的实例
    private ImageView getFloatDragView(View.OnClickListener clickListener) {
        if (mImageView != null) {
            return mImageView;
        } else {
            mImageView = new ImageView(context);
            mImageView.setClickable(true);
            mImageView.setFocusable(true);
            mImageView.setImageResource(R.drawable.fail_work);
            setFloatDragViewParams(mImageView);
            mImageView.setOnClickListener(clickListener);
            setFloatDragViewTouch(mImageView);
            return mImageView;
        }

    }

    // 设置可拖动按钮的位置参数
    private void setFloatDragViewParams(View floatDragView) {

        // 记录最后图片在窗体的位置
        int moveX = lastPosition[0];
        int moveY = lastPosition[1];
        if (0 != moveX || 0 != moveY) {// 移动后的位置
            // 每次移动都要设置其layout，不然由于父布局可能嵌套listView，当父布局发生改变冲毁（如下拉刷新时）则移动的view会回到原来的位置
            RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lpFeedback.setMargins(moveX, moveY, 0, 0);
            floatDragView.setLayoutParams(lpFeedback);
        } else {// 初始位置
            RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lpFeedback.setMargins(0, 0, 20, 218);
            lpFeedback.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lpFeedback.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            floatDragView.setLayoutParams(lpFeedback);
        }

    }

    // 可拖动按钮的touch事件
    private void setFloatDragViewTouch(final ImageView floatDragView) {

        floatDragView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(final View v, MotionEvent event) {

                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        isIntercept = false;
                        startDownX = relativeMoveX = (int) event.getRawX();
                        startDownY = relativeMoveY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - relativeMoveX;
                        int dy = (int) event.getRawY() - relativeMoveY;

                        int left = v.getLeft() + dx;
                        int top = v.getTop() + dy;
                        int right = v.getRight() + dx;
                        int bottom = v.getBottom() + dy;
                        if (left < 0) {
                            left = 0;
                            right = left + v.getWidth();
                        }
                        if (right > mScreenWidth) {
                            right = mScreenWidth;
                            left = right - v.getWidth();
                        }
                        if (top < 0) {
                            top = 0;
                            bottom = top + v.getHeight();
                        }
                        if (bottom > mScreenHeight) {
                            bottom = mScreenHeight;
                            top = bottom - v.getHeight();
                        }
                        v.layout(left, top, right, bottom);
                        relativeMoveX = (int) event.getRawX();
                        relativeMoveY = (int) event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        int lastMoveDx = Math.abs((int) event.getRawX() - startDownX);
                        int lastMoveDy = Math.abs((int) event.getRawY() - startDownY);

                        if (5 < lastMoveDx || 5 < lastMoveDy) {// 防止点击的时候稍微有点移动点击事件被拦截了
                            isIntercept = true;
                        } else {
                            isIntercept = false;
                        }
                        // 每次移动都要设置其layout，不然由于父布局可能嵌套listview，
                        // 当父布局发生改变冲毁（如下拉刷新时）则移动的view会回到原来的位置
                        RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        lpFeedback.setMargins(v.getLeft(), v.getTop(), 0, 0);

                        v.setLayoutParams(lpFeedback);
                        // preferenceUtil.saveInt("moveX", v.getLeft());
                        // preferenceUtil.saveInt("moveY", v.getTop());
                        // 设置靠近边沿的
                        setImageViewNearEdge(v);
                        break;
                }
                return isIntercept;
            }
        });
    }

    // 将拖动按钮移动到边沿
    private void setImageViewNearEdge(final View v) {

        if (v.getLeft() < ((Utils.getScreenSize(context).x) / 2)) {
            // 设置位移动画 向左移动控件位置
            final TranslateAnimation animation = new TranslateAnimation(0, -v.getLeft(), 0, 0);
            animation.setDuration(300);// 设置动画持续时间
            animation.setRepeatCount(0);// 设置重复次数
            animation.setFillAfter(true);
            animation.setRepeatMode(Animation.ABSOLUTE);

            animation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation arg0) {
                    // TODO: 2017/3/1
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                    // TODO: 2017/3/1
                }

                @Override
                public void onAnimationEnd(Animation arg0) {
                    v.clearAnimation();
                    RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lpFeedback.setMargins(0, v.getTop(), 0, 0);
                    v.setLayoutParams(lpFeedback);
                    v.postInvalidateOnAnimation();
                    lastPosition[0] = 0;
                    lastPosition[1] = v.getTop();
                }
            });
            v.startAnimation(animation);

        } else {
            final TranslateAnimation animation = new TranslateAnimation(0, (Utils.getScreenSize(context).x
                    - v.getLeft() - v.getWidth()), 0, 0);
            animation.setDuration(300);// 设置动画持续时间
            animation.setRepeatCount(0);// 设置重复次数
            animation.setRepeatMode(Animation.ABSOLUTE);
            animation.setFillAfter(true);
            animation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation arg0) {
                    // TODO: 2017/3/1
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                    // TODO Auto-generated method stub
                }

                @Override
                public void onAnimationEnd(Animation arg0) {
                    v.clearAnimation();
                    RelativeLayout.LayoutParams lpFeedback = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lpFeedback.setMargins(Utils.getScreenSize(context).x - v.getWidth(), v.getTop(), 0, 0);
                    v.setLayoutParams(lpFeedback);
                    v.postInvalidateOnAnimation();
                    lastPosition[0] =Utils.getScreenSize(context).x - v.getWidth();
                    lastPosition[1] = v.getTop();
                }
            });
            v.startAnimation(animation);
        }
    }

    // 计算屏幕的实际高宽
    private void setScreenHW(Activity context) {
        if (mScreenHeight < 0) {
            // 减去状态栏高度，否则挨着底部移动，导致图标变小
            Point screen = Utils.getScreenSize(context);
            mScreenWidth = screen.x;
            mScreenHeight = screen.y - Utils.getStatusBarHeight(context);
        }
    }

}

