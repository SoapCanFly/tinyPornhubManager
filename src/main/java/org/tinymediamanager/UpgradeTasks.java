/*
 * Copyright 2012 - 2019 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager;

import static org.tinymediamanager.core.Utils.deleteFileSafely;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.MovieSettings;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowSettings;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.sun.jna.Platform;

/**
 * The class UpdateTasks. To perform needed update tasks
 *
 * @author Manuel Laggner / Myron Boyle
 */
public class UpgradeTasks {
  private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeTasks.class);

  public static void performUpgradeTasksBeforeDatabaseLoading(String oldVersion) {
    String v = "" + oldVersion;
    if (StringUtils.isBlank(v)) {
      v = "3"; // set version for other updates
    }

    // ****************************************************
    // PLEASE MAKE THIS TO RUN MULTIPLE TIMES WITHOUT ERROR
    // NEEDED FOR NIGHTLY SNAPSHOTS ET ALL
    // SVN BUILD IS ALSO CONSIDERED AS LOWER !!!
    // ****************************************************

    // upgrade to v3 (OR DO THIS IF WE ARE INSIDE IDE)
    // if (StrgUtils.compareVersion(v, "3") < 0) {
    // LOGGER.info("Performing upgrade tasks to version 3");
    // }

  }

  /**
   * performs some upgrade tasks from one version to another<br>
   * <b>make sure, this upgrade can run multiple times (= needed for nightlies!!!)
   *
   * @param oldVersion our current version
   */
  public static void performUpgradeTasksAfterDatabaseLoading(String oldVersion) {
    MovieList movieList = MovieList.getInstance();
    TvShowList tvShowList = TvShowList.getInstance();

    String v = "" + oldVersion;

    if (StringUtils.isBlank(v)) {
      v = "3"; // set version for other updates
    }

    // ****************************************************
    // PLEASE MAKE THIS TO RUN MULTIPLE TIMES WITHOUT ERROR
    // NEEDED FOR NIGHTLY SNAPSHOTS ET ALL
    // GIT BUILD IS ALSO CONSIDERED AS LOWER !!!
    // ****************************************************

    // upgrade to v3.0
    if (StrgUtils.compareVersion(v, "3.0.0") < 0) {
      LOGGER.info("Performing database upgrade tasks to version 3");
      // clean old style backup files
      ArrayList<Path> al = new ArrayList<>();

      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(Globals.BACKUP_FOLDER))) {
        for (Path path : directoryStream) {
          if (path.getFileName().toString().matches("movies\\.db\\.\\d{4}\\-\\d{2}\\-\\d{2}\\.zip")
                  || path.getFileName().toString().matches("tvshows\\.db\\.\\d{4}\\-\\d{2}\\-\\d{2}\\.zip")) {
            al.add(path);
          }
        }
      } catch (IOException ignored) {
      }

      for (Path path : al) {
        deleteFileSafely(path);
      }

      // has been expanded to space
      if (MovieSettings.getInstance().getRenamerColonReplacement().equals("")) {
        MovieSettings.getInstance().setRenamerColonReplacement(" ");
        MovieSettings.getInstance().saveSettings();
      }
      if (TvShowSettings.getInstance().getRenamerColonReplacement().equals("")) {
        TvShowSettings.getInstance().setRenamerColonReplacement(" ");
        TvShowSettings.getInstance().saveSettings();
      }
    }

    // upgrade to v3.0.1
    if (StrgUtils.compareVersion(v, "3.0.1") < 0) {
      LOGGER.info("Performing database upgrade tasks to version 3.0.1");
      // remove the tvShowSeason id from TV shows
      for (TvShow tvShow : TvShowList.getInstance().getTvShows()) {
        if (tvShow.getIds().containsKey("tvShowSeason")) {
          tvShow.removeId("tvShowSeason");
          tvShow.saveToDb();
        }
      }

      // remove "http://thetvdb.com/banners/" artwork urls from episodes
      for (TvShow tvShow : TvShowList.getInstance().getTvShows()) {
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          if (episode.getArtworkUrl(MediaFileType.THUMB).equals("http://thetvdb.com/banners/")) {
            episode.setArtworkUrl("", MediaFileType.THUMB);
            episode.saveToDb();
          }
        }
      }
    }

    // upgrade to v3.0.2
    if (StrgUtils.compareVersion(v, "3.0.2") < 0) {
      LOGGER.info("Performing database upgrade tasks to version 3.0.2");
      // set episode year
      for (TvShow tvShow : TvShowList.getInstance().getTvShows()) {
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          if (episode.getYear() == 0 && episode.getFirstAired() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(episode.getFirstAired());
            episode.setYear(calendar.get(Calendar.YEAR));
            episode.saveToDb();
          }
        }
      }
    }

    // add stream flags for old booleans
    if (StrgUtils.compareVersion(v, "3.0.3") < 0) {
      LOGGER.info("Performing database upgrade tasks to version 3.0.3");

      for (Movie movie : MovieList.getInstance().getMovies()) {
        boolean dirty = false;
        for (MediaFile mf : movie.getMediaFiles()) {
          for (MediaFileAudioStream as : mf.getAudioStreams()) {
            // the IS method checks already for new field
            if (as.defaultStream && !as.isDefaultStream()) {
              as.setDefaultStream(true);
              dirty = true;
            }
          }
          for (MediaFileSubtitle sub : mf.getSubtitles()) {
            // the IS method checks already for new field
            if (sub.defaultStream && !sub.isDefaultStream()) {
              sub.setDefaultStream(true);
              dirty = true;
            }
            if (sub.forced && !sub.isForced()) {
              sub.setForced(true);
              dirty = true;
            }
          }
        }
        if (dirty) {
          movie.saveToDb();
        }
      }

      for (TvShow tvShow : TvShowList.getInstance().getTvShows()) {
        for (TvShowEpisode episode : tvShow.getEpisodes()) {
          boolean dirty = false;
          for (MediaFile mf : episode.getMediaFiles()) {
            for (MediaFileAudioStream as : mf.getAudioStreams()) {
              // the IS method checks already for new field
              if (as.defaultStream && !as.isDefaultStream()) {
                as.setDefaultStream(true);
                dirty = true;
              }
            }
            for (MediaFileSubtitle sub : mf.getSubtitles()) {
              // the IS method checks already for new field
              if (sub.defaultStream && !sub.isDefaultStream()) {
                sub.setDefaultStream(true);
                dirty = true;
              }
              if (sub.forced && !sub.isForced()) {
                sub.setForced(true);
                dirty = true;
              }
            }
          }
          if (dirty) {
            episode.saveToDb();
          }
        }
      }
    }

    // migrate image cache to hex folders
    if (StrgUtils.compareVersion(v, "3.0.4") < 0) {
      LOGGER.info("Performing database upgrade tasks to version 3.0.4");
      ImageCache.migrate();

      //change unknown file extension to regex
      for (int i = 0; i < Settings.getInstance().getCleanupFileType().size(); i++) {
        Settings.getInstance().getCleanupFileType().set(i, Settings.getInstance().getCleanupFileType().get(i) + "$");
      }
    }
  }

  /**
   * rename downloaded files (getdown.jar, ...)
   */
  public static void renameDownloadedFiles() {
    // self updater
    File file = new File("getdown-new.jar");
    if (file.exists() && file.length() > 100000) {
      File cur = new File("getdown.jar");
      if (file.length() != cur.length() || !cur.exists()) {
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update the updater!");
        }
      }
    }

    // exe launchers
    if (Platform.isWindows()) {
      file = new File("tinyMediaManager.new");
      if (file.exists() && file.length() > 10000 && file.length() < 100000) {
        File cur = new File("tinyMediaManager.exe");
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update tmm!");
        }
      }
      file = new File("tinyMediaManagerUpd.new");
      if (file.exists() && file.length() > 10000 && file.length() < 100000) {
        File cur = new File("tinyMediaManagerUpd.exe");
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update the updater!");
        }
      }
      file = new File("tinyMediaManagerCMD.new");
      if (file.exists() && file.length() > 10000 && file.length() < 100000) {
        File cur = new File("tinyMediaManagerCMD.exe");
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update CMD TMM!");
        }
      }
    }

    // OSX launcher
    if (Platform.isMac()) {
      file = new File("JavaApplicationStub.new");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../../MacOS/JavaApplicationStub");
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update JavaApplicationStub");
        }
      }
    }

    // OSX Info.plist
    if (Platform.isMac()) {
      file = new File("Info.plist");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../../Info.plist");
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update JavaApplicationStub");
        }
      }
    }

    // OSX tmm.icns
    if (Platform.isMac()) {
      file = new File("tmm.icns");
      if (file.exists() && file.length() > 0) {
        File cur = new File("../tmm.icns");
        try {
          FileUtils.copyFile(file, cur);
        } catch (IOException e) {
          LOGGER.error("Could not update tmm.icns");
        }
      }
    }
  }
}
