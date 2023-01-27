package tech.glasgowneuro.attysscope2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Overlay which prints all the infos on the screen in a semi transparent
 * scope look.
 */
public class InfoView extends View {

    static private String TAG = "InfoView";

    static private final Paint paintLarge = new Paint();
    static private final Paint paintSmall = new Paint();
    static private final Paint paintRec = new Paint();
    static private int textHeight = 0;
    static private String largeText;
    static private String smallText;
    static private String recText;
    static Rect bounds = new Rect();

    public InfoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public InfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InfoView(Context context) {
        super(context);
        init();
    }

    private void init() {
        paintLarge.setColor(Color.argb(128, 0, 255, 0));
        paintSmall.setColor(Color.argb(128, 0, 255, 0));
        paintRec.setColor(Color.argb(128,255,0,0));
    }

    public int getInfoHeight() {
        return textHeight;
    }

    public void resetInfoHeight() { textHeight = 0;}

    public void drawText(String _largeText, String _smallText, String _rectxt) {
        largeText = _largeText;
        smallText = _smallText;
        recText = _rectxt;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int yLarge = 0;
        int xLarge = 0;
        int width = getWidth();
        int txtDiv = 25;
        do {
            paintSmall.setTextSize((float)getHeight() / txtDiv);
            if (null != smallText) {
                paintSmall.getTextBounds(smallText + "|y`", 0, smallText.length(), bounds);
            }
            txtDiv++;
        } while ((width - (bounds.width() * 10 / 9)) < 0);
        int y2 = bounds.height();
        if (largeText != null) {
            if (largeText.length()>0) {
                int txtDivTmp = 7;
                do {
                    paintLarge.setTextSize((float)getHeight() / txtDivTmp);
                    paintLarge.getTextBounds(largeText+"|y`", 0, largeText.length(), bounds);
                    xLarge = width - (bounds.width() * 10 / 9);
                    txtDivTmp++;
                } while (xLarge < 0);
                String dummyText = "1.2424Vpp";
                paintLarge.getTextBounds(dummyText, 0, dummyText.length(), bounds);
                yLarge = bounds.height();
                canvas.drawText(largeText, xLarge, yLarge + (float)y2 * 10 / 9, paintLarge);
            }
        }
        if (null != smallText) {
            canvas.drawText(smallText, (float)width / 100, y2, paintSmall);
        }
        if ((y2+yLarge)>textHeight) {
            textHeight = y2 + yLarge;
        }
        if (null != recText) {
            paintRec.setTextSize((float)getHeight() / 30);
            canvas.drawText(recText, (float)width / 50, getHeight() - (float)bounds.height() / 4, paintRec);
        }
    }
}
