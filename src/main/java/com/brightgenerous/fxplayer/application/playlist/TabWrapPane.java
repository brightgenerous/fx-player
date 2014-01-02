package com.brightgenerous.fxplayer.application.playlist;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

public class TabWrapPane extends Pane {

    {
        getStyleClass().add("tab-wrap-pane");
    }

    private final DoubleProperty tabHeight = new SimpleDoubleProperty(this, "tabHeight", 31);

    private final BooleanProperty visibleTab = new SimpleBooleanProperty(this, "visibleTab");

    {
        tabHeight.addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue,
                    Number newValue) {
                requestLayout();
            }
        });
        visibleTab.addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                    Boolean newValue) {
                requestLayout();
            }
        });
    }

    @Override
    protected void layoutChildren() {

        TabPane content = getContent(true);

        if (content == null) {
            // list is null
            return;
        }

        double rightInset = snapSpace(getInsets().getRight());
        double leftInset = snapSpace(getInsets().getLeft());
        double topInset = snapSpace(getInsets().getTop());
        double bottomInset = snapSpace(getInsets().getBottom());

        double actualWidth = getWidth() - leftInset - rightInset;
        double actualHeight = getHeight() - topInset - bottomInset;

        if (visibleTab.get()) {
            layoutInArea(content, leftInset, topInset, actualWidth, actualHeight, 0, HPos.CENTER,
                    VPos.CENTER);
        } else {
            Side side = content.getSide();
            double height = tabHeight.get();
            if (side == Side.LEFT) {
                layoutInArea(content, leftInset - height, topInset, actualWidth + height,
                        actualHeight, 0, HPos.CENTER, VPos.CENTER);
            } else if (side == Side.RIGHT) {
                layoutInArea(content, leftInset, topInset, actualWidth + height, actualHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            } else {
                layoutInArea(content, leftInset, topInset, actualWidth, actualHeight, 0,
                        HPos.CENTER, VPos.CENTER);
            }
        }
    }

    public TabPane getContent() {
        return getContent(false);
    }

    private TabPane getContent(boolean managed) {
        TabPane ret = null;
        List<Node> children = managed ? getManagedChildren() : getChildren();
        for (Node node : children) {
            if (node instanceof TabPane) {
                ret = (TabPane) node;
            }
        }
        return ret;
    }

    public void setContent(TabPane view) {
        List<TabPane> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node instanceof TabPane) {
                dels.add((TabPane) node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        if (view != null) {
            children.add(view);
        }
    }

    public DoubleProperty tabHeightProperty() {
        return tabHeight;
    }

    public double getTabHeight() {
        return tabHeight.get();
    }

    public void setTabHeight(double height) {
        tabHeight.set(height);
    }

    public BooleanProperty visibleTabProperty() {
        return visibleTab;
    }

    public boolean getVisibleTab() {
        return visibleTab.get();
    }

    public void setVisibleTab(boolean visible) {
        visibleTab.set(visible);
    }
}
