//
// Copyright 2016 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.9
//
package co.koriel.yonapp.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import co.amazonaws.mobile.AWSMobileClient;
import co.amazonaws.mobile.content.ContentDownloadPolicy;
import co.amazonaws.mobile.content.ContentItem;
import co.amazonaws.mobile.content.ContentListHandler;
import co.amazonaws.mobile.content.ContentManager;
import co.amazonaws.mobile.content.ContentState;
import co.amazonaws.mobile.util.S3Utils;
import co.amazonaws.mobile.util.StringFormatUtils;
import co.koriel.yonapp.R;
import co.koriel.yonapp.fragment.content.ContentListItem;
import co.koriel.yonapp.fragment.content.ContentListViewAdapter;
import co.koriel.yonapp.fragment.content.ContentUtils;
import co.koriel.yonapp.util.ContentHelper;

public class ContentDeliveryFragment extends FragmentBase implements AdapterView.OnItemClickListener {

    /** Content Manager that manages the content for this demo. */
    private ContentManager contentManager;

    /** The current relative path within the ContentManager. */
    private String currentPath = "";

    /** Text view for showing the current path. */
    private TextView pathTextView;

    /** Text view for showing the maximum number of bytes for the cache. */
    private TextView cacheLimitTextView;

    /** Text view for showing the number of bytes in use in the cache. */
    private TextView cacheInUseTextView;

    /** Text view for showing the number of bytes available in the cache. */
    private TextView cacheAvailableTextView;

    /** Text view for showing the number of bytes pinned in the cache. */
    private TextView cachePinnedTextView;

    /** Menu Text for refreshing content. */
    private String refreshText;

    /** Menu Text for downloading recent content. */
    private String downloadRecentText;

    /** Menu Text for setting the cache size. */
    private String setCacheSizeText;

    /** Menu Text for clearing the cache. */
    private String clearCacheText;

    /** Handles the main content list. */
    private ContentListViewAdapter contentListItems;

    /** The list view for displaying the list of files. */
    private ListView listView;

    private SwipeRefreshLayout swipeRefreshLayout;

    /** Flag to keep track of whether currently listing content. */
    private volatile boolean listingContentInProgress = false;

    private void createContentList(final View fragmentView, final ContentManager contentManager) {
        listView = (ListView) fragmentView.findViewById(android.R.id.list);
        contentListItems = new ContentListViewAdapter(fragmentView.getContext(), contentManager,
            new ContentListViewAdapter.ContentListPathProvider() {
                @Override
                public String getCurrentPath() {
                    return currentPath;
                }
            },
            new ContentListViewAdapter.ContentListCacheObserver() {
                @Override
                public void onCacheChanged() {
                    refreshCacheSummary();
                }
            },
            R.layout.fragment_demo_content_delivery);
        listView.setAdapter(contentListItems);
        listView.setOnItemClickListener(this);
        listView.setOnCreateContextMenuListener(this);
        listView.setOnScrollListener(OnScrollChange);
    }

    private void updatePath() {
        pathTextView.setText(getString(R.string.content_path_prefix_text)
                + (currentPath.isEmpty() ? "./" : currentPath));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshText = getString(R.string.content_refresh_text);
        downloadRecentText = getString(R.string.content_download_recent_text);
        setCacheSizeText = getString(R.string.content_set_cache_size_text);
        clearCacheText = getString(R.string.content_clear_cache_text);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment.
        final View fragmentView = inflater.inflate(
            R.layout.fragment_demo_content_delivery, container, false);

        TextView toolbarTitle = (TextView) getActivity().findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("솔루션 클라우드");

        swipeRefreshLayout = (SwipeRefreshLayout) fragmentView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(OnRefreshLayout);

        pathTextView = (TextView) fragmentView.findViewById(R.id.content_path);
        cacheLimitTextView = (TextView) fragmentView.findViewById(R.id.content_cache_limit_value);
        cacheInUseTextView = (TextView) fragmentView.findViewById(R.id.content_cache_use_value);
        cacheAvailableTextView = (TextView) fragmentView.findViewById(R.id.content_cache_available_value);
        cachePinnedTextView = (TextView) fragmentView.findViewById(R.id.content_cache_pinned_value);

        registerForContextMenu(cacheLimitTextView);

        return fragmentView;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {

        final ProgressDialog dialog = getProgressDialog(
            R.string.content_progress_dialog_message_load_local_content);

        AWSMobileClient.defaultMobileClient().
                createDefaultContentManager(new ContentManager.BuilderResultHandler() {

                    @Override
                    public void onComplete(final ContentManager contentManager) {
                        if (!isAdded()) {
                            contentManager.destroy();
                            return;
                        }

                        final View fragmentView = getView();
                        ContentDeliveryFragment.this.contentManager = contentManager;
                        createContentList(fragmentView, contentManager);
                        contentManager.setContentRemovedListener(contentListItems);
                        dialog.dismiss();
                        refreshContent(currentPath);
                    }
                });
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshLayout = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshContent(currentPath);
        }
    };

    private AbsListView.OnScrollListener OnScrollChange = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            boolean enable = false;
            if(listView != null && listView.getChildCount() > 0){
                // check if the first item of the list is visible
                boolean firstItemVisible = listView.getFirstVisiblePosition() == 0;
                // check if the top of the first item is visible
                boolean topOfFirstItemVisible = listView.getChildAt(0).getTop() == 0;
                // enabling or disabling the refresh layout
                enable = firstItemVisible && topOfFirstItemVisible;
            }
            swipeRefreshLayout.setEnabled(enable);
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        if (contentManager != null) {
            // Remove any Progress listeners that may be registered.
            contentManager.clearAllListeners();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (contentManager != null) {
            contentManager.setContentRemovedListener(contentListItems);
            refreshContent(currentPath);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(item.getItemId()) {
            case R.id.content_context_menu_refresh:
                return true;
            case R.id.content_context_menu_download_recent:
                contentManager.downloadRecentContent(currentPath, contentListItems);
                return true;
            case R.id.content_context_menu_set_cache_size:
                final View fragmentView = getView();
                if (fragmentView != null) {
                    // Show selector for cache size.
                    getActivity().openContextMenu(cacheLimitTextView);
                }
                return true;
            case R.id.content_context_menu_clear_cache:
                contentManager.clearCache();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view == cacheLimitTextView) {
            for (final SpannableString string : ContentUtils.cacheSizeStrings) {
                menu.add(string).setActionView(view);
            }
            menu.setHeaderTitle(ContentUtils.getCenteredString(setCacheSizeText));
        } else if (view.getId() == listView.getId()) {
            final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            final ContentItem contentItem = contentListItems.getItem(info.position).getContentItem();
            final ContentState contentState = contentItem.getContentState();

            final boolean isNewerVersionAvailable =
                ContentState.isCachedWithNewerVersionAvailableOrTransferring(contentState);
            final boolean isCached = contentState == ContentState.CACHED || isNewerVersionAvailable;
            final boolean isPinned = contentManager.isContentPinned(contentItem.getFilePath());

            if (contentState == ContentState.REMOTE_DIRECTORY) {
                final String newPath;
                if (currentPath.equals(contentItem.getFilePath())) {
                    newPath = S3Utils.getParentDirectory(currentPath);
                } else {
                    newPath = contentItem.getFilePath();
                }

                refreshContent(newPath);
                return;
            }
            // if item is downloaded
            if (isCached) {
                menu.add(0,
                        R.id.content_context_menu_open,
                        1, // menu item order
                        getString(R.string.content_context_menu_open)).setActionView(listView);
                menu.add(0,
                        R.id.content_context_menu_delete_local,
                        7, // menu item order
                        getString(R.string.content_context_menu_delete_local)).setActionView(listView);
                if (!isPinned) {
                    menu.add(0,
                            R.id.content_context_menu_pin,
                            4, // menu item order
                            getString(R.string.content_context_menu_pin)).setActionView(listView);
                }
            } else {
                menu.add(0,
                        R.id.content_context_menu_download,
                        2, // menu item order
                        getString(R.string.content_context_menu_download)).setActionView(listView);
                menu.add(0,
                        R.id.content_context_menu_download_pin,
                        5, // menu item order
                        getString(R.string.content_context_menu_download_pin)).setActionView(listView);
            }
            if (isNewerVersionAvailable) {
                menu.add(0,
                        R.id.content_context_menu_download_latest,
                        3, // menu item order
                        getString(R.string.content_context_menu_download_latest)).setActionView(listView);
            }
            if (isPinned) {
                menu.add(0,
                        R.id.content_context_menu_unpin,
                        6, // menu item order
                        getString(R.string.content_context_menu_unpin)).setActionView(listView);
            }

            menu.setHeaderTitle(contentItem.getFilePath());
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        if (item.getActionView() == cacheLimitTextView) {
            final SpannableString itemTitle = (SpannableString) item.getTitle();
            final int cacheSizeIndex = ContentUtils.cacheSizeStrings.indexOf(itemTitle);
            if (cacheSizeIndex > -1) {
                contentManager.setContentCacheSize(ContentUtils.cacheSizeValues[cacheSizeIndex]);
                refreshCacheSummary();
            }
            return true;
        } else if (item.getActionView() == listView) {
            final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final ContentListItem listItem = contentListItems.getItem(info.position);
            final ContentItem contentItem = listItem.getContentItem();

            switch(item.getItemId()) {
                case R.id.content_context_menu_open:
                    ContentHelper.openFileViewer(getActivity(), contentItem.getFile());
                    return true;
                case R.id.content_context_menu_download:
                    // Download the content.
                    contentManager.getContent(contentItem.getFilePath(), contentItem.getSize(),
                            ContentDownloadPolicy.DOWNLOAD_ALWAYS, false, contentListItems);
                    return true;
                case R.id.content_context_menu_download_latest:
                    // Download the content.
                    contentManager.getContent(contentItem.getFilePath(), contentItem.getSize(),
                            ContentDownloadPolicy.DOWNLOAD_IF_NEWER_EXIST, false, contentListItems);
                    return true;
                case R.id.content_context_menu_download_pin:
                    contentManager.getContent(contentItem.getFilePath(), contentItem.getSize(),
                            ContentDownloadPolicy.DOWNLOAD_ALWAYS, true, contentListItems);
                    return true;
                case R.id.content_context_menu_pin:
                    contentManager.pinContent(contentItem.getFilePath(), contentListItems);
                    return true;
                case R.id.content_context_menu_unpin:
                    contentManager.unPinContent(contentItem.getFilePath(), new Runnable() {
                        @Override
                        public void run() {
                            refreshCacheSummary();
                            contentListItems.notifyDataSetChanged();
                        }
                    });
                    return true;
                case R.id.content_context_menu_delete_local:
                    contentManager.removeLocal(contentItem.getFilePath());
                    return true;
            }
        }

        return false;
    }

    @Override
    public void onDestroyView() {

        unregisterForContextMenu(cacheLimitTextView);

        if (contentManager != null) {
            contentManager.destroy();
        }

        super.onDestroyView();
    }

    private ProgressDialog getProgressDialog(final int resId, Object... args) {
        return ProgressDialog.show(getActivity(),
            getString(R.string.content_progress_dialog_title_wait),
            getString(resId, (Object[]) args));
    }

    private void refreshCacheSummary() {
        // Load the item selected from the current cache size.
        final long limitBytes = contentManager.getContentCacheSize();
        final long usedBytes = contentManager.getCacheUsedSize();
        final long pinnedBytes = contentManager.getPinnedSize();

        cacheLimitTextView.setText(StringFormatUtils.getBytesString(limitBytes, false));
        cacheInUseTextView.setText(StringFormatUtils.getBytesString(usedBytes, false));
        cacheAvailableTextView.setText(StringFormatUtils.getBytesString(
            limitBytes - usedBytes, false));
        cachePinnedTextView.setText(StringFormatUtils.getBytesString(pinnedBytes, false));
    }

    public void refreshContent(final String path) {
        if (!listingContentInProgress) {
            listingContentInProgress = true;

            refreshCacheSummary();

            // Remove all progress listeners.
            contentManager.clearProgressListeners();

            // Clear old content.
            contentListItems.clear();
            contentListItems.notifyDataSetChanged();
            currentPath = path;
            updatePath();

            final ProgressDialog dialog = getProgressDialog(
                R.string.content_progress_dialog_message_load_content);

            contentManager.listAvailableContent(path, new ContentListHandler() {
                @Override
                public boolean onContentReceived(final int startIndex,
                                                 final List<ContentItem> partialResults,
                                                 final boolean hasMoreResults) {
                    // if the activity is no longer alive, we can stop immediately.
                    if (getActivity() == null) {
                        listingContentInProgress = false;
                        return false;
                    }
                    if (startIndex == 0) {
                        dialog.dismiss();
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    for (final ContentItem contentItem : partialResults) {
                        // Add the item to the list.
                        contentListItems.add(new ContentListItem(contentItem));

                        // If the content is transferring, ensure the progress listener is set.
                        final ContentState contentState = contentItem.getContentState();
                        if (ContentState.isTransferringOrWaitingToTransfer(contentState)) {
                            contentManager.setProgressListener(contentItem.getFilePath(),
                                contentListItems);
                        }
                    }
                    // sort items added.
                    contentListItems.sort(ContentListItem.contentAlphebeticalComparator);

                    if (!hasMoreResults) {
                        listingContentInProgress = false;
                    }
                    // Return true to continue listing.
                    return true;
                }

                @Override
                public void onError(final Exception ex) {
                    dialog.dismiss();
                    swipeRefreshLayout.setRefreshing(false);
                    listingContentInProgress = false;
                    final Activity activity = getActivity();
                    if (activity != null) {
                        final AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(activity);
                        errorDialogBuilder.setTitle(activity.getString(R.string.content_list_failure_text));
                        errorDialogBuilder.setMessage(ex.getMessage());
                        errorDialogBuilder.setNegativeButton(
                            activity.getString(R.string.content_dialog_ok), null);
                        errorDialogBuilder.show();
                    }
                }
            });
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        listView.showContextMenuForChild(view);
    }
}
