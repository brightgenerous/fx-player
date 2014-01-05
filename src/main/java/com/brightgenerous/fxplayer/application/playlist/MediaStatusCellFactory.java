package com.brightgenerous.fxplayer.application.playlist;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import com.brightgenerous.fxplayer.media.MediaInfo;
import com.brightgenerous.fxplayer.media.MediaStatus;

public class MediaStatusCellFactory implements
        Callback<TableColumn<MediaInfo, MediaStatus>, TableCell<MediaInfo, MediaStatus>> {

    @Override
    public TableCell<MediaInfo, MediaStatus> call(TableColumn<MediaInfo, MediaStatus> param) {
        return new TableCell<MediaInfo, MediaStatus>() {

            @Override
            protected void updateItem(MediaStatus item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || (item == null)) {
                    setText(null);
                } else {
                    String str;
                    switch (item) {
                        case MEDIA_YET:
                            str = String.format("- %d",
                                    Integer.valueOf(getTableRow().getIndex() + 1));
                            break;
                        case MEDIA_SUCCESS:
                            str = String
                                    .format("%d", Integer.valueOf(getTableRow().getIndex() + 1));
                            break;
                        case MEDIA_ERROR:
                            str = String.format("x %d",
                                    Integer.valueOf(getTableRow().getIndex() + 1));
                            break;
                        case PLAYER_LOADING:
                        case PLAYER_READY:
                        case PLAYER_PLAYING:
                        case PLAYER_PAUSE:
                            str = String.format("+ %d",
                                    Integer.valueOf(getTableRow().getIndex() + 1));
                            break;
                        case PLAYER_END:
                            str = String
                                    .format("%d", Integer.valueOf(getTableRow().getIndex() + 1));
                            break;
                        default:
                            str = "";
                            break;
                    }
                    setText(str);
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        };
    }
}
