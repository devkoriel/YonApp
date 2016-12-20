package co.koriel.yonapp.fragment.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import co.koriel.yonapp.R;

public class NoticeAdapter extends BaseAdapter {
    private ArrayList<NoticeItem> noticeItems;

    public NoticeAdapter() {
        this.noticeItems = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return this.noticeItems.size();
    }

    @Override
    public Object getItem(int i) {
        return this.noticeItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final Context context = viewGroup.getContext();

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.notice_item, viewGroup, false);
        }

        TextView markTextView = (TextView) view.findViewById(R.id.notice_mark);
        TextView titleTextView = (TextView) view.findViewById(R.id.notice_title);
        TextView dateTextView = (TextView) view.findViewById(R.id.notice_date);

        NoticeItem noticeItem = this.noticeItems.get(i);

        markTextView.setText(noticeItem.getTitle().substring(1, 2));
        titleTextView.setText(noticeItem.getTitle());
        dateTextView.setText(noticeItem.getDate());

        return view;
    }

    public void addItem(NoticeItem noticeItem) {
        this.noticeItems.add(noticeItem);
    }

    public void addItem(int index, NoticeItem noticeItem) {
        this.noticeItems.add(index, noticeItem);
    }

    public void addAllItems(ArrayList<NoticeItem> noticeItems) {
        this.noticeItems.addAll(noticeItems);
    }

    public void addAllItems(int index, ArrayList<NoticeItem> noticeItems) {
        this.noticeItems.addAll(index, noticeItems);
    }

    public void removeItem(int index) {
        this.noticeItems.remove(index);
    }

    public void clearItems() {
        this.noticeItems.clear();
    }
}
