package com.brightgenerous.fxplayer.application.playlist;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class StoreVBox extends VBox {

    private final BooleanProperty hideHeader = new SimpleBooleanProperty(this, "hideHeader");

    private final BooleanProperty hideFooter = new SimpleBooleanProperty(this, "hideFooter");

    {
        hideHeader.addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                    Boolean newValue) {
                requestLayout();
            }
        });

        hideFooter.addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                    Boolean newValue) {
                requestLayout();
            }
        });
    }

    private Node header;

    private Node footer;

    private Node content;

    public StoreVBox() {
    }

    public StoreVBox(double spacing) {
        super(spacing);
    }

    @Override
    public List<Node> getManagedChildren() {
        List<Node> ret = new ArrayList<>();
        for (Node node : super.getManagedChildren()) {
            if (node == header) {
                if (!getHideHeader()) {
                    ret.add(node);
                }
            } else if (node == footer) {
                if (!getHideFooter()) {
                    ret.add(node);
                }
            } else if (node == content) {
                ret.add(node);
            }
        }
        return ret;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if ((header != null) && getHideHeader()) {
            layoutInArea(header, 0, 0, 0, 0, 0, HPos.CENTER, VPos.BOTTOM);
        }
        if ((footer != null) && getHideFooter()) {
            layoutInArea(footer, 0, getHeight(), 0, 0, 0, HPos.CENTER, VPos.TOP);
        }
    }

    public double getHeaderHeight() {
        if (header == null) {
            return -1;
        }
        if (header instanceof Region) {
            return ((Region) header).getHeight();
        }
        if (header instanceof Control) {
            return ((Control) header).getHeight();
        }
        return -1;
    }

    public Node getHeader() {
        return getHeader(false);
    }

    private Node getHeader(boolean managed) {
        if (header == null) {
            return null;
        }
        return (managed && !header.isManaged()) ? null : header;
    }

    public void setHeader(Node view) {
        List<Node> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node == header) {
                dels.add(node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        header = view;
        if (view != null) {
            children.add(0, view);
        }
    }

    public double getFooterHeight() {
        if (footer == null) {
            return -1;
        }
        if (footer instanceof Region) {
            return ((Region) footer).getHeight();
        }
        if (footer instanceof Control) {
            return ((Control) footer).getHeight();
        }
        return -1;
    }

    public Node getFooter() {
        return getFooter(false);
    }

    private Node getFooter(boolean managed) {
        if (footer == null) {
            return null;
        }
        return (managed && !footer.isManaged()) ? null : footer;
    }

    public void setFooter(Node view) {
        List<Node> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node == footer) {
                dels.add(node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        footer = view;
        if (view != null) {
            children.add(view);
        }
    }

    public Node getContent() {
        return getContent(false);
    }

    private Node getContent(boolean managed) {
        if (content == null) {
            return null;
        }
        return (managed && !content.isManaged()) ? null : content;
    }

    public void setContent(Node view) {
        List<Node> dels = new ArrayList<>();
        List<Node> children = getChildren();
        for (Node node : children) {
            if (node == content) {
                dels.add(node);
            }
        }
        if (!dels.isEmpty()) {
            children.removeAll(dels);
        }
        content = view;
        if (view != null) {
            if (children.isEmpty()) {
                children.add(view);
            } else if (header == null) {
                children.add(0, view);
            } else {
                children.add(1, view);
            }
        }
    }

    public BooleanProperty hideHeaderProperty() {
        return hideHeader;
    }

    public boolean getHideHeader() {
        return hideHeader.get();
    }

    public void setHideHeader(boolean hide) {
        hideHeader.set(hide);
    }

    public BooleanProperty hideFooterProperty() {
        return hideFooter;
    }

    public boolean getHideFooter() {
        return hideFooter.get();
    }

    public void setHideFooter(boolean hide) {
        hideFooter.set(hide);
    }
}
