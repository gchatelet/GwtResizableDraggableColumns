package fr.mikrosimage.gwt.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

public class ResizableHeader<T> extends Header<String> {
    private static final Style.Cursor moveCursor = Cursor.POINTER;
    private static final Style.Cursor resizeCursor = Cursor.COL_RESIZE;
    private static final String RESIZE_COLOR = "#A49AED";
    private static final String MOVE_COLOR = "gray";
    private static final int MINIMUM_COLUMN_WIDTH = 30;
    private static final int RESIZE_HANDLE_WIDTH = 10;
    private static final int CARET_WIDTH = 4;
    private static final double GHOST_OPACITY = .3;
    private final String title;
    private final Column<T, ?> column;
    private final Document document = Document.get();
    private final DivElement caret = document.createDivElement();
    private final Style caretStyle = caret.getStyle();
    private final CellTable<T> table;
    private Element lastTarget;

    public ResizableHeader(String title, CellTable<T> table, Column<T, ?> column) {
        super(new HeaderCell());
        this.title = title;
        this.column = column;
        this.table = table;
        caretStyle.setPosition(Position.ABSOLUTE);
        caretStyle.setTop(0, Unit.PX);
        caretStyle.setBottom(0, Unit.PX);
        caretStyle.setWidth(CARET_WIDTH, Unit.PX);
        caretStyle.setBackgroundColor("transparent");
        caretStyle.setZIndex(1000);
    }

    @Override
    public String getValue() {
        return title;
    }

    @Override
    public void onBrowserEvent(Context context, Element target, NativeEvent event) {
        final String eventType = event.getType();
        final int clientX = event.getClientX();
        final int absoluteLeft = target.getAbsoluteLeft();
        final int offsetWidth = target.getOffsetWidth();
        final int headerHeight = target.getOffsetHeight();
        final boolean resizing = clientX > absoluteLeft + offsetWidth - RESIZE_HANDLE_WIDTH;
        event.preventDefault();
        event.stopPropagation();
        if ("mousedown".equals(eventType)) {
            if (resizing)
                new ColumnResizeHelper(target, event);
            else
                new ColumnMoverHelper(target, event);
        } else if ("mousemove".equals(eventType)) {
            if (lastTarget != target)
                caret.removeFromParent();
            if (!caret.hasParentElement())
                target.appendChild(caret);
            caretStyle.setHeight(headerHeight, Unit.PX);
            caretStyle.setTop(table.getAbsoluteTop(), Unit.PX);
            if (resizing) {
                setCursor(target, resizeCursor);
                caretStyle.setBackgroundColor(RESIZE_COLOR);
                caretStyle.setLeft(absoluteLeft + offsetWidth - CARET_WIDTH, Unit.PX);
                caretStyle.setWidth(CARET_WIDTH, Unit.PX);
                caretStyle.setOpacity(1);
            } else { //moving
                setCursor(target, moveCursor);
                caretStyle.setBackgroundColor(MOVE_COLOR);
                caretStyle.setOpacity(GHOST_OPACITY);
                caretStyle.setLeft(absoluteLeft, Unit.PX);
                caretStyle.setWidth(offsetWidth, Unit.PX);
                caretStyle.setHeight(headerHeight, Unit.PX);
            }
        } else if ("mouseout".equals(eventType)) {
            setCursor(target, Cursor.DEFAULT);
            caretStyle.setBackgroundColor("transparent");
        }
        this.lastTarget = target;
    }

    private static void setCursor(Element element, Cursor cursor) {
        element.getStyle().setCursor(cursor);
    }

    class ColumnResizeHelper implements NativePreviewHandler {
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final DivElement resizeLine = document.createDivElement();
        private final Style resizeLineStyle = resizeLine.getStyle();
        private final Element target;

        public ColumnResizeHelper(Element target, NativeEvent event) {
            this.target = target;
            setLine(resizeLineStyle, 1, target.getAbsoluteTop() + target.getOffsetHeight(), table.getBodyHeight(), RESIZE_COLOR);
            moveLine(event.getClientX());
            target.appendChild(resizeLine);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            final NativeEvent nativeEvent = event.getNativeEvent();
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();
            final int absoluteLeft = target.getAbsoluteLeft();
            final int clientX = nativeEvent.getClientX();
            final String eventType = nativeEvent.getType();
            if ("mousemove".equals(eventType)) {
                moveLine(clientX);
            } else if ("mouseup".equals(eventType)) {
                handler.removeHandler();
                resizeLine.removeFromParent();
                caretStyle.setBackgroundColor("transparent");
                final int newWidth = Math.max(clientX - absoluteLeft, MINIMUM_COLUMN_WIDTH);
                if (table != null)
                    table.setColumnWidth(column, newWidth + "px");
            }
        }

        private void moveLine(final int clientX) {
            resizeLineStyle.setLeft(clientX, Unit.PX);
            caretStyle.setLeft(clientX - CARET_WIDTH / 2, Unit.PX);
        }
    }

    class ColumnMoverHelper implements NativePreviewHandler {
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final DivElement ghostLine = document.createDivElement();
        private final Style ghostLineStyle = ghostLine.getStyle();
        private final DivElement ghostColumn = document.createDivElement();
        private final Style ghostColumnStyle = ghostColumn.getStyle();
        private final int columnWidth;
        private final int[] columnXPositions;
        private int fromIndex = -1;
        private int toIndex;

        public ColumnMoverHelper(Element target, NativeEvent event) {
            final int clientX = event.getClientX();
            columnWidth = target.getOffsetWidth();
            final Element tr = target.getParentElement();
            final int columns = tr.getChildCount();
            columnXPositions = new int[columns + 1];
            columnXPositions[0] = tr.getAbsoluteLeft();
            for (int i = 0; i < columns; ++i) {
                final int xPos = columnXPositions[i] + ((Element) tr.getChild(i)).getOffsetWidth();
                if (xPos > clientX && fromIndex == -1)
                    fromIndex = i;
                columnXPositions[i + 1] = xPos;
            }
            toIndex = fromIndex;
            final int top = table.getAbsoluteTop() + target.getOffsetHeight();
            final int bodyHeight = table.getBodyHeight();
            setLine(ghostColumnStyle, columnWidth, top, bodyHeight, MOVE_COLOR);
            setLine(ghostLineStyle, 3, top, bodyHeight, RESIZE_COLOR);
            ghostColumnStyle.setOpacity(GHOST_OPACITY);
            moveColumn(clientX);
            target.appendChild(ghostColumn);
            target.appendChild(ghostLine);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            final NativeEvent nativeEvent = event.getNativeEvent();
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();
            final String eventType = nativeEvent.getType();
            if ("mousemove".equals(eventType)) {
                moveColumn(nativeEvent.getClientX());
            } else if ("mouseup".equals(eventType)) {
                handler.removeHandler();
                ghostColumn.removeFromParent();
                ghostLine.removeFromParent();
                caretStyle.setBackgroundColor("transparent");
                if (table != null && fromIndex != toIndex) {
                    table.removeColumn(fromIndex);
                    table.insertColumn(toIndex, column, ResizableHeader.this);
                }
            }
        }

        private void moveColumn(final int clientX) {
            final int pointer = clientX - columnWidth / 2;
            ghostColumnStyle.setLeft(pointer, Unit.PX);
            for (int i = 0; i < columnXPositions.length - 1; ++i) {
                if (clientX < columnXPositions[i + 1]) {
                    final int adjustedIndex = i > fromIndex ? i + 1 : i;
                    ghostLineStyle.setLeft(columnXPositions[adjustedIndex], Unit.PX);
                    toIndex = i;
                    break;
                }
            }
        }
    }

    private static void setLine(Style style, int width, int top, int height, String color) {
        style.setPosition(Position.ABSOLUTE);
        style.setTop(top, Unit.PX);
        style.setHeight(height, Unit.PX);
        style.setWidth(width, Unit.PX);
        style.setBackgroundColor(color);
        style.setZIndex(1000);
    }

    static class HeaderCell extends AbstractCell<String> {
        public HeaderCell() {
            super("click", "mousedown", "mousemove", "mouseout");
        }

        @Override
        public void render(Context context, String value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value));
        }
    }
};
