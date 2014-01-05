package com.brightgenerous.fxplayer.application.playlist;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import javafx.util.Duration;

import com.brightgenerous.fxplayer.media.MediaInfo;

public class DurationCellFactory implements
        Callback<TableColumn<MediaInfo, Duration>, TableCell<MediaInfo, Duration>> {

    @Override
    public TableCell<MediaInfo, Duration> call(TableColumn<MediaInfo, Duration> param) {

        return new TableCell<MediaInfo, Duration>() {

            @Override
            protected void updateItem(Duration item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || (item == null)) {
                    setText(null);
                } else {
                    setText(LabelUtils.milliSecToTime(item.toMillis()));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }
}
