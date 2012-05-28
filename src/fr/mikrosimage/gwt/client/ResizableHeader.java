package fr.mikrosimage.gwt.client;

import static com.google.gwt.dom.client.Style.Unit.PX;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

public abstract class ResizableHeader<T> extends Header<String> {
    private static final Style.Cursor moveCursor = Cursor.POINTER;
    private static final Style.Cursor resizeCursor = Cursor.COL_RESIZE;
    private static final String RESIZE_COLOR = "#A49AED";
    private static final String MOVE_COLOR = "gray";
    private static final double GHOST_OPACITY = .3;
    private static final int MINIMUM_COLUMN_WIDTH = 30;
    private static final int RESIZE_HANDLE_WIDTH = 10;
    private static final int CARET_WIDTH = 4;
    private final String title;
    private final Column<T, ?> column;
    private final Document document = Document.get();
    private final DivElement caret = document.createDivElement();
    private final Style caretStyle = caret.getStyle();
    private final AbstractCellTable<T> table;
    private final Element tableElement;

    public ResizableHeader(String title, AbstractCellTable<T> table, Column<T, ?> column) {
        super(new HeaderCell());
        if (title == null || table == null || column == null)
            throw new NullPointerException();
        this.title = title;
        this.column = column;
        this.table = table;
        this.tableElement = table.getElement();
        caretStyle.setPosition(Position.ABSOLUTE);
        caretStyle.setTop(0, PX);
        caretStyle.setBottom(0, PX);
        caretStyle.setWidth(CARET_WIDTH, PX);
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
        final int headerLeft = target.getOffsetLeft();
        final int headerWidth = target.getOffsetWidth();
        final boolean resizing = clientX > target.getAbsoluteLeft() + headerWidth - RESIZE_HANDLE_WIDTH;
        event.preventDefault();
        event.stopPropagation();
        if ("mousedown".equals(eventType)) {
            if (resizing)
                new ColumnResizeHelper(target, event);
            else
                new ColumnMoverHelper(target, event);
        } else if ("mousemove".equals(eventType)) {
            caret.removeFromParent();
            target.appendChild(caret);
            caretStyle.setHeight(target.getOffsetHeight(), PX);
            caretStyle.setTop(target.getOffsetTop(), PX);
            if (resizing) {
                setCursor(target, resizeCursor);
                caretStyle.setBackgroundColor(RESIZE_COLOR);
                caretStyle.setLeft(headerLeft + headerWidth - CARET_WIDTH, PX);
                caretStyle.setWidth(CARET_WIDTH, PX);
                caretStyle.setOpacity(1);
            } else { //moving
                setCursor(target, moveCursor);
                caretStyle.setBackgroundColor(MOVE_COLOR);
                caretStyle.setOpacity(GHOST_OPACITY);
                caretStyle.setLeft(headerLeft, PX);
                caretStyle.setWidth(headerWidth, PX);
            }
        } else if ("mouseout".equals(eventType)) {
            setCursor(target, Cursor.DEFAULT);
            caretStyle.setBackgroundColor("transparent");
        }
    }

    private static void setCursor(Element element, Cursor cursor) {
        element.getStyle().setCursor(cursor);
    }

    private class ColumnResizeHelper implements NativePreviewHandler {
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final DivElement resizeLine = document.createDivElement();
        private final Style resizeLineStyle = resizeLine.getStyle();
        private final Element target;

        private ColumnResizeHelper(Element target, NativeEvent event) {
            this.target = target;
            setLine(resizeLineStyle, 1, target.getAbsoluteTop() + target.getOffsetHeight(), getTableBodyHeight(), RESIZE_COLOR);
            moveLine(event.getClientX());
            tableElement.appendChild(resizeLine);
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
                columnResized(newWidth);
            }
        }

        private void moveLine(final int clientX) {
            final int xPos = clientX - table.getAbsoluteLeft();
            resizeLineStyle.setLeft(xPos, PX);
            resizeLineStyle.setTop(target.getOffsetHeight(), PX);
            caretStyle.setLeft(xPos - CARET_WIDTH / 2, PX);
        }
    }

    private class ColumnMoverHelper implements NativePreviewHandler {
        private static final int ghostLineWidth = 2;
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final DivElement ghostLine = document.createDivElement();
        private final Style ghostLineStyle = ghostLine.getStyle();
        private final DivElement ghostColumn = document.createDivElement();
        private final Style ghostColumnStyle = ghostColumn.getStyle();
        private final int columnWidth;
        private final int[] columnXPositions;
        private int fromIndex = -1;
        private int toIndex;

        private ColumnMoverHelper(Element target, NativeEvent event) {
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
            final int top = target.getOffsetHeight();
            final int bodyHeight = getTableBodyHeight();
            setLine(ghostColumnStyle, columnWidth, top, bodyHeight, MOVE_COLOR);
            setLine(ghostLineStyle, ghostLineWidth, top, bodyHeight, RESIZE_COLOR);
            ghostColumnStyle.setOpacity(GHOST_OPACITY);
            moveColumn(clientX);
            tableElement.appendChild(ghostColumn);
            tableElement.appendChild(ghostLine);
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
                if (fromIndex != toIndex)
                    columnMoved(fromIndex, toIndex);
            }
        }

        private void moveColumn(final int clientX) {
            final int pointer = clientX - columnWidth / 2;
            ghostColumnStyle.setLeft(pointer - table.getAbsoluteLeft(), PX);
            for (int i = 0; i < columnXPositions.length - 1; ++i) {
                if (clientX < columnXPositions[i + 1]) {
                    final int adjustedIndex = i > fromIndex ? i + 1 : i;
                    ghostLineStyle.setLeft(columnXPositions[adjustedIndex] - table.getAbsoluteLeft() - ghostLineWidth / 2, PX);
                    toIndex = i;
                    break;
                }
            }
        }
    }

    private static void setLine(Style style, int width, int top, int height, String color) {
        style.setPosition(Position.ABSOLUTE);
        style.setTop(top, PX);
        style.setHeight(height, PX);
        style.setWidth(width, PX);
        style.setBackgroundColor(color);
        style.setZIndex(1000);
    }

    private static class HeaderCell extends AbstractCell<String> {
        public HeaderCell() {
            super("click", "mousedown", "mousemove", "mouseout");
        }

        @Override
        public void render(Context context, String value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value));
        }
    }

    protected void columnResized(int newWidth) {
        table.setColumnWidth(column, newWidth + "px");
    }

    protected void columnMoved(int fromIndex, int toIndex) {
        table.removeColumn(fromIndex);
        table.insertColumn(toIndex, column, this);
    }

    protected abstract int getTableBodyHeight();
};
