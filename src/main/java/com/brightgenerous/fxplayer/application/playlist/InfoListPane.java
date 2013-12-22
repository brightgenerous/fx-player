package com.brightgenerous.fxplayer.application.playlist;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

public class InfoListPane extends Pane {

    @Override
    protected void layoutChildren() {

        Node list = getInfoList(true);

        if (list == null) {
            // list is null
            return;
        }

        double rightInset = snapSpace(getInsets().getRight());
        double leftInset = snapSpace(getInsets().getLeft());
        double topInset = snapSpace(getInsets().getTop());
        double bottomInset = snapSpace(getInsets().getBottom());

        double actualWidth = getWidth() - leftInset - rightInset;
        double actualHeight = getHeight() - topInset - bottomInset;

        layoutInArea(list, leftInset, topInset, actualWidth, actualHeight, 0, HPos.CENTER,
                VPos.CENTER);
    }

    public TableView<?> getInfoList() {
        return getInfoList(false);
    }

    private TableView<?> getInfoList(boolean managed) {
        TableView<?> ret = null;
        List<Node> children = managed ? getManagedChildren() : getChildren();
        for (Node node : children) {
            if (node instanceof TableView) {
                ret = (TableView<?>) node;
            }
        }
        return ret;
    }

    public void setInfoList(TableView<?> view) {
        List<TableView<?>> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node instanceof TableView) {
                dels.add((TableView<?>) node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        if (view != null) {
            children.add(view);
        }
    }
}
