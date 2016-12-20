package co.koriel.yonapp.fragment.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import co.amazonaws.mobile.AWSMobileClient;
import co.koriel.yonapp.R;

public class OneLineBoardCommentAdapter extends BaseAdapter {
    private ArrayList<OneLineBoardCommentItem> oneLineBoardCommentItems;
    private String userId;

    public OneLineBoardCommentAdapter() {
        this.oneLineBoardCommentItems = new ArrayList<>();
        this.userId = AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID();
    }

    @Override
    public int getCount() {
        return this.oneLineBoardCommentItems.size();
    }

    @Override
    public Object getItem(int i) {
        return this.oneLineBoardCommentItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final int position = i;
        final Context context = viewGroup.getContext();

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.oneline_comment_listview_item, viewGroup, false);
        }

        final TextView idTextView = (TextView) view.findViewById(R.id.text_comment_id);
        TextView timeBeforeTextView = (TextView) view.findViewById(R.id.text_comment_time);
        TextView indexTextView = (TextView) view.findViewById(R.id.text_index);
        TextView commentTextView = (TextView) view.findViewById(R.id.text_comment);

        final OneLineBoardCommentItem oneLineBoardCommentItem = this.oneLineBoardCommentItems.get(position);

        final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);

        final Animation fadeOutIdText = new AlphaAnimation(1.0f, 0.0f);
        fadeOutIdText.setDuration(500);
        fadeOutIdText.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                idTextView.setText(oneLineBoardCommentItem.getId());
                idTextView.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        String preString = idTextView.getText().toString();
        String postString = oneLineBoardCommentItem.getId();

        if (postString.length() != 0) {
            if (!preString.equals(postString)) {
                if (preString.length() == 0) {
                    idTextView.setText(postString);
                    idTextView.startAnimation(fadeIn);
                } else {
                    idTextView.startAnimation(fadeOutIdText);
                }
            } else {
                idTextView.setText(postString);
            }
        }

        timeBeforeTextView.setText(oneLineBoardCommentItem.getTimeBefore());
        indexTextView.setText(Integer.toString(oneLineBoardCommentItem.getIndex()));
        commentTextView.setText(oneLineBoardCommentItem.getComment());

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (this.oneLineBoardCommentItems.get(position).getCommentDateAndId().split("]")[1].equals(userId)) {
            return 0;
        } else {
            return 1;
        }
    }

    public void addItem(OneLineBoardCommentItem oneLineBoardCommentItem) {
        this.oneLineBoardCommentItems.add(oneLineBoardCommentItem);
    }

    public void addItem(int index, OneLineBoardCommentItem oneLineBoardCommentItem) {
        this.oneLineBoardCommentItems.add(index, oneLineBoardCommentItem);
    }

    public void addAllItems(ArrayList<OneLineBoardCommentItem> oneLineBoardCommentItems) {
        this.oneLineBoardCommentItems.addAll(oneLineBoardCommentItems);
    }

    public void addAllItems(int index, ArrayList<OneLineBoardCommentItem> oneLineBoardCommentItems) {
        this.oneLineBoardCommentItems.addAll(index, oneLineBoardCommentItems);
    }

    public void removeItem(int index) {
        this.oneLineBoardCommentItems.remove(index);
    }

    public void clearItems() {
        this.oneLineBoardCommentItems.clear();
    }
}