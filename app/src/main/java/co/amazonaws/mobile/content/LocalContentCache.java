//
// Copyright 2016 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.9
//
package co.amazonaws.mobile.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import co.amazonaws.mobile.util.ThreadUtils;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/* package */ class LocalContentCache implements Iterable<File> {
    private static final String LOG_TAG = LocalContentCache.class.getSimpleName();
    private static final String PREF_KEY_MAX_CACHE_SIZE = "maxCacheSize";

    /** A map for cached files accounting. */
    private final Map<String, CachedFileEntry> cachedFilesByRelativeFilePath;

    private final TreeMap<CachedFileEntry, File> orderedCachedFileEntries;

    /** Bytes currently stored in the cache. */
    private volatile long bytesUsed;

    /** Bytes currently stored that have their pinned flag set (aren't counting toward bytesUsed). */
    private volatile long bytesPinned;

    /** The maximum number of bytes the cache can accomodate. */
    private long maxCacheSize;

    /** Shared preferences for maintaining the cache size. */
    private final SharedPreferences prefs;

    private final SharedPreferences pinnedFilePrefs;

    /** Listener handlers for when content is removed or an error occurs removing content. */
    private ContentRemovedListener contentRemovedListener;

    /** Path to local content, does not contain trailing slash. */
    final String localContentPath;

    /* An entry with information concerning a cached file. */
    private class CachedFileEntry {
        /** The underlying file in the cache. */
        private final File file;
        private final String relativeFilePath;
        /** Keep track of the size so that accounting for bytes used in the cache does not get
         *  broken if a file is removed externally (without this cache removing it). */
        private final long size;
        /** Keep track of the last modified time to remove the oldest files first. */
        private final long lastModifiedTime;
        /** true if pinned and in cache, otherwise false. */
        private volatile boolean isPinned;

        CachedFileEntry(final File file, final String relativeFilePath, final long size,
                        final long lastModifiedTime) {
            this.file = file;
            this.relativeFilePath = relativeFilePath;
            this.size = size;
            this.lastModifiedTime = lastModifiedTime;

            isPinned = shouldPinFile(relativeFilePath);
            if (isPinned) {
                // If the file was not previously flagged as in the cache
                if (!pinnedFilePrefs.getBoolean(relativeFilePath, false)) {
                    // Set its boolean value to true to indicate it is pinned and in cache.
                    pinnedFilePrefs.edit().putBoolean(relativeFilePath, true).apply();
                }
            }
        }
    }

    private static final Comparator<CachedFileEntry> cachedEntriesComparator
        = new Comparator<CachedFileEntry>() {
        @Override
        public int compare(CachedFileEntry lhs, CachedFileEntry rhs) {
            final long rlm = rhs.lastModifiedTime;
            final long llm = lhs.lastModifiedTime;

            // Sort oldest last modified first.
            if (rlm != llm) {
                return rlm < llm ? 1 : -1;
            }

            // Sort by smallest file size first.
            if (rhs.size != lhs.size) {
                return rhs.size < lhs.size ? 1 : -1;
            }

            // Sort alphabetically descending.
            return rhs.relativeFilePath.compareTo(lhs.relativeFilePath);
        }
    };

    /**
     * Constructs the local content cache. This should be constructed off of the main thread.
     *
     * @param context android context.
     * @param sharedPrefName base name for shared preferences used by this class.
     * @param localContentPath path at which to store the local content.
     */
    public LocalContentCache(final Context context,
                             final String sharedPrefName,
                             final String localContentPath) {
        this.localContentPath = localContentPath;
        cachedFilesByRelativeFilePath = new HashMap<>();
        orderedCachedFileEntries = new TreeMap<>(cachedEntriesComparator);
        contentRemovedListener = null;
        bytesUsed = 0;
        bytesPinned = 0;

        prefs = context.getSharedPreferences(sharedPrefName,
            Context.MODE_PRIVATE);
        pinnedFilePrefs = context.getSharedPreferences(sharedPrefName + "_pinned_files",
            Context.MODE_PRIVATE);

        // Initially the cache size starts with the maximum possible size.  Once it is set it is
        // from then on loaded from shared preferences.
        maxCacheSize = Long.MAX_VALUE;

        final File localDir = new File(localContentPath);
        if (!localDir.exists()) {
            if (!localDir.mkdir()) {
                throw new RuntimeException(String.format(
                    "Local content path '%s' doesn't exist and the directory can not be created.",
                    localContentPath));
            }
        }
        if (!localDir.isDirectory()) {
            throw new RuntimeException(
                String.format("Local content path '%s' is not a directory.", localContentPath));
        }

        maxCacheSize = prefs.getLong(PREF_KEY_MAX_CACHE_SIZE, Long.MAX_VALUE);
        // refresh the content in the local cache in the background.
        refreshLocalContent();
    }

    private void removeBytesForAddingFile(final File file) {
        // Remove files if we have gone above the cache size
        final long bytesOverSize = bytesUsed - maxCacheSize;
        if (bytesOverSize > 0) {
            Log.d(LOG_TAG, String.format("Cache over size limit. Freeing %d bytes to store %s.",
                bytesOverSize, file.getName()));
            removeBytes(bytesOverSize);
        }
    }
    /**
     * Adds a file to the local cache.
     * All callers of this private method synchronize on this object to maintain thread safety.
     * @param file the File to add.
     */
    private void addFile(final String relativeFilePath, final File file) {
        final long lastModifiedTime = file.lastModified();
        long fileSize = file.length();
        final CachedFileEntry fileEntry = new CachedFileEntry(file, relativeFilePath,
            fileSize, lastModifiedTime);

        orderedCachedFileEntries.put(fileEntry, file);
        cachedFilesByRelativeFilePath.put(relativeFilePath, fileEntry);
        if (!fileEntry.isPinned) {
            bytesUsed += fileSize;
        } else {
            bytesPinned += fileSize;
        }
        removeBytesForAddingFile(file);
    }

    /*package*/ String absolutePathToRelativePath(final String absolutePath) {
        return absolutePath.substring(localContentPath.length()+1);
    }
    private void addDir(final File localDir) {
        // Iterate through the content dir and add all files.
        for (final File file : localDir.listFiles()) {
            if (file.isDirectory()) {
                addDir(file);
            } else {
                // Get the file path by subtracting off the localContentPath (+1 due to trailing slash).
                final String filePath = absolutePathToRelativePath(file.getAbsolutePath());
                addFile(filePath, file);
            }
        }
    }
    /**
     * Refreshes the local content by reading the file system. This happens only on construction.
     */
    private synchronized void refreshLocalContent() {
        final File localDir = new File(localContentPath);
        bytesUsed = 0;
        addDir(localDir);
    }

    /**
     * @return An iterator that provides all the local content in order by last modified time.
     */
    @Override
    public Iterator<File> iterator() {
        Queue<File> cachedFiles = new ConcurrentLinkedQueue<>();

        synchronized (this) {
            // Copy the items into the queue in age order.
            for (final File file : orderedCachedFileEntries.descendingMap().values()) {
                cachedFiles.add(file);
            }
        }

        return cachedFiles.iterator();
    }

    public Iterable<File> getIterableForDirectory(final String directoryPath) {
        Queue<File> cachedFiles = new ConcurrentLinkedQueue<>();

        synchronized (this) {
            // Copy the items into the queue in age order.
            for (final File file : orderedCachedFileEntries.descendingMap().values()) {
                final String relativePath = absolutePathToRelativePath(file.getAbsolutePath());
                // if the file is in the directory
                if (relativePath.startsWith(directoryPath)) {
                    // if there is not another directory after the given path.
                    if (relativePath.indexOf(TransferHelper.DIR_DELIMITER, directoryPath.length()) == -1) {
                        cachedFiles.add(file);
                    }
                }
            }
        }

        return cachedFiles;
    }

    private synchronized void removeNonExistingFileEntry(final CachedFileEntry cachedFileEntry) {
        final File cachedFile = cachedFileEntry.file;
        // Remove the file that no longer exists from the cache.
        orderedCachedFileEntries.remove(cachedFileEntry);

        // Remove the bytes being used by the cache for this item.
        if (!cachedFileEntry.isPinned) {
            bytesUsed -= cachedFileEntry.size;
        } else {
            bytesPinned -= cachedFileEntry.size;
        }
        cachedFilesByRelativeFilePath.remove(cachedFileEntry.relativeFilePath);
    }

    /**
     * Retrieve a file from the local cache.
     * @param filePath the file name.
     * @return the File object.
     */
    public synchronized File get(String filePath) {
        final CachedFileEntry cachedFileEntry = cachedFilesByRelativeFilePath.get(filePath);
        if (cachedFileEntry != null) {
            final File cachedFile = cachedFileEntry.file;
            // check whether the file still exists in the file system.
            if (cachedFile.exists()) {
                return cachedFile;
            }
            removeNonExistingFileEntry(cachedFileEntry);
        }
        return null;
    }

    /**
     * Check if the cache contains a particular file object.
     * @param filePath the file name.
     * @return true if the file is in the cache, otherwise returns false.
     */
    public boolean contains(final String filePath) {
        return (get(filePath) != null);
    }

    /**
     * Adds a file to the cache by moving it into the cache directory.
     * Note: This currently only works for files stored on the same mount point. Meaning, this will
     * not work to move a file from an SD card into a cache directory in device storage.
     *
     * @param incommingFile reference to the file to add into the cache.
     * @return A reference to the moved file.
     * @throws IOException thrown if an existing file of the same name cannot be removed to be
     *                     replaced or if the file cannot be moved such as due to a permission error
     *                     or attempt to move a file stored on another partition.
     */
    public synchronized File addByMoving(final String relativeFilePath, final File incommingFile) throws IOException {
        final File cachedFile = new File(localContentPath + "/" + relativeFilePath);
        final CachedFileEntry entry = cachedFilesByRelativeFilePath.get(relativeFilePath);
        // if this item is in our cache.
        if (entry != null) {
            // Remove the file if it exists.
            if (cachedFile.exists()) {
                if (!cachedFile.delete()) {
                    if (contentRemovedListener != null) {
                        contentRemovedListener.onRemoveError(cachedFile);
                        incommingFile.delete();
                        throw new IOException(String.format(
                            "can't remove the existing file '%s' from cache to be replaced.",
                            relativeFilePath));
                    }
                }
            }
            removeNonExistingFileEntry(entry);
        }
        // if the relative path contained a directory
        if (relativeFilePath.contains(TransferHelper.DIR_DELIMITER)) {
            // ensure the relative path exists
            final File containingDir = new File(localContentPath + "/" +
                relativeFilePath.substring(0, relativeFilePath.lastIndexOf("/")));
            if (!containingDir.exists()) {
                if (!containingDir.mkdirs()) {
                    throw new IOException(String.format(
                        "Can't create the containing directory to save '%s'.",
                        relativeFilePath));
                }
            } else if (!containingDir.isDirectory()) {
                throw new IOException(String.format(
                    "The containing directory to save '%s' is not a directory.",
                    relativeFilePath));
            }
        }

        if (!incommingFile.renameTo(cachedFile)) {
            if (!incommingFile.delete()) {
                Log.d(LOG_TAG, String.format("Couldn't delete incomming file '%s'.", relativeFilePath));
            }
            throw new IOException(String.format("Can't move file '%s' into the local cache.",
                relativeFilePath));
        }
        addFile(relativeFilePath, cachedFile);
        return cachedFile;
    }


    /**
     * Pin a file in the cache but don't count it toward the used cache size.
     * @param filePath the file name.
     * @return true if the file being pinned is in the cache, otherwise false.
     */
    public synchronized boolean pinFile(final String filePath) {
        // if the file exists in the cache
        final CachedFileEntry cachedFileEntry = cachedFilesByRelativeFilePath.get(filePath);
        final boolean isFileInCache = cachedFileEntry != null;
        if (isFileInCache) {
            // Reduce size used in cache since pinned files don't count toward the size.
            bytesUsed -= cachedFileEntry.size;
            bytesPinned += cachedFileEntry.size;
            cachedFileEntry.isPinned = true;
        }
        pinnedFilePrefs.edit().putBoolean(filePath, isFileInCache).apply();
        return isFileInCache;
    }

    /**
     * Unpin a file in the cache and count it toward the used cache size.
     * @param filePath the relative file path and file name.
     */
    public synchronized void unPinFile(final String filePath) {
        if (!pinnedFilePrefs.contains(filePath)) {
            return;
        }
        pinnedFilePrefs.edit().remove(filePath).apply();

        final CachedFileEntry cachedFileEntry = cachedFilesByRelativeFilePath.get(filePath);

        // if the file exists in the cache
        if (cachedFileEntry != null) {
            final File cachedFile = cachedFileEntry.file;
            if (cachedFile.exists()) {
                cachedFileEntry.isPinned = false;
                // Warn if the cached file length is different than what we have accounted for.
                if (cachedFileEntry.size != cachedFile.length()) {
                    Log.w(LOG_TAG, String.format(
                        "cached file size unexpectedly changed, expected %d bytes, found %d bytes",
                        cachedFileEntry.size, cachedFile.length()));
                }
                bytesPinned -= cachedFileEntry.size;
                bytesUsed += cachedFileEntry.size;
                removeBytesForAddingFile(cachedFileEntry.file);
                return;
            }
            // Deliberately not setting cachedFileEntry.pinned to false, since the entry is being
            // removed entirely.
            removeNonExistingFileEntry(cachedFileEntry);
        }
    }

    public Set<String> getPinnedFilePathSet() {
        Map<String, ?> allPrefs = pinnedFilePrefs.getAll();
        HashSet<String> pinnedFiles = new HashSet<>(allPrefs.size());
        for (String relativeFilePath : allPrefs.keySet()) {
            pinnedFiles.add(relativeFilePath);
        }
        return pinnedFiles;
    }

    /**
     * Determine if a file has been pinned using {@link #pinFile(String)}. See also
     * {@link #unPinFile(String)} to unpin a file.
     *
     * @param filePath the relative path and file name.
     * @return true if the file is pinned, otherwise false.
     */
    public boolean shouldPinFile(final String filePath) {
        // if the key exists in the shared preferences, it should be pinned.
        return pinnedFilePrefs.contains(filePath);
    }

    /**
     * @param filePath the relative path and file name.
     * @return true if the file is currently held in the cache and pinned, otherwise false.
     */
    public synchronized boolean isFileInCacheAndPinned(final String filePath) {
        final CachedFileEntry cachedFileEntry = cachedFilesByRelativeFilePath.get(filePath);

        // if the file exists in the cache
        if (cachedFileEntry != null) {
            return cachedFileEntry.isPinned;
        }
        return false;
    }

    /**
     * @return the total number of bytes stored locally for pinned files (that don't counting toward
     *         bytes used in the cache).
     */
    public long getBytesPinned() {
        return bytesPinned;
    }

    /**
     * Remove at least the specified number of bytes from the cache.
     * @param bytes The minimum number of bytes to remove from the cache.
     * @return the number of files removed from the cache.
     */
    public synchronized int removeBytes(final long bytes) {
        long remainingBytesToRemove = bytes;

        // iterate through the cached files by last accessed ascending (oldest first)
        Iterator<CachedFileEntry> fileEntryIterator = orderedCachedFileEntries.keySet().iterator();
        int removedCount = 0;

        while (fileEntryIterator.hasNext()) {
            final CachedFileEntry fileEntry = fileEntryIterator.next();
            final File file = fileEntry.file;

            if (!fileEntry.isPinned) {
                bytesUsed -= fileEntry.size;
                cachedFilesByRelativeFilePath.remove(fileEntry.relativeFilePath);
                fileEntryIterator.remove();
                remainingBytesToRemove -= fileEntry.size;
                removedCount++;
                if (file.exists() && !file.delete()) {
                    Log.e(LOG_TAG, "Couldn't delete file from cache: "
                        + file.getAbsolutePath());
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (contentRemovedListener != null) {
                                contentRemovedListener.onRemoveError(file);
                            }
                        }
                    });
                }

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (contentRemovedListener != null) {
                            contentRemovedListener.onFileRemoved(file);
                        }
                    }
                });

                if (remainingBytesToRemove <= 0) {
                    break;
                }
            }
        }
        return removedCount;
    }

    /**
     * Removes a file from the local cache. Also un-pins the file if it was pinned.
     * @param filePath the relative file path.
     * @return true if the file will be removed asynchronously, otherwise false, if the file
     *         is not in the local cache.
     */
    public synchronized boolean removeFile(final String filePath) {
        final CachedFileEntry fileEntry = cachedFilesByRelativeFilePath.get(filePath);
        if (fileEntry == null) {
            return false;
        }

        // Remove file in background.
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File file = fileEntry.file;
                cachedFilesByRelativeFilePath.remove(fileEntry.relativeFilePath);
                orderedCachedFileEntries.remove(fileEntry);
                // if the file is pinned
                if (pinnedFilePrefs.contains(filePath)) {
                    // unpin the file.
                    pinnedFilePrefs.edit().remove(filePath).apply();
                    bytesPinned -= fileEntry.size;
                } else {
                    // adjust bytes used.
                    bytesUsed -= fileEntry.size;
                }
                if (file.exists() && !file.delete()) {
                    Log.e(LOG_TAG, "Couldn't delete file from cache: "
                        + file.getAbsolutePath());
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (contentRemovedListener != null) {
                                contentRemovedListener.onRemoveError(file);
                            }
                        }
                    });
                }

                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (contentRemovedListener != null) {
                            contentRemovedListener.onFileRemoved(file);
                        }
                    }
                });
            }
        }).start();

        return true;
    }

    /**
     * @return the number of bytes currently used in the cache.
     */
    public long getCacheSizeUsed() {
        return bytesUsed;
    }

    /**
     * @return the maximum number of bytes this cache may hold.
     */
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Sets the maximum number of bytes this cache may hold.  Performs file operations and should
     * be called on a background thread.
     * @param maxCacheSize the maximum number of bytes this cache may hold.
     * @return the number of files removed from the cache.
     */
    public int setMaxCacheSize(final long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        prefs.edit()
            .putLong(PREF_KEY_MAX_CACHE_SIZE, maxCacheSize)
            .apply();
        if (bytesUsed > maxCacheSize) {
            return removeBytes(bytesUsed - maxCacheSize);
        }
        return 0;
    }

    public void clear() {
        // remove all non-pinned items from cache.
        removeBytes(bytesUsed);
    }

    public ContentRemovedListener getContentRemovedListener() {
        return contentRemovedListener;
    }

    public void setContentRemovedListener(ContentRemovedListener contentRemovedListener) {
        this.contentRemovedListener = contentRemovedListener;
    }
}
