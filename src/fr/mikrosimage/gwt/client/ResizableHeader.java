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
    private final String title;
    private final Document document = Document.get();
    private final AbstractCellTable<T> table;
    private final Element tableElement;
    protected final Column<T, ?> column;

    public ResizableHeader(String title, AbstractCellTable<T> table, Column<T, ?> column) {
        super(new HeaderCell());
        if (title == null || table == null || column == null)
            throw new NullPointerException();
        this.title = title;
        this.column = column;
        this.table = table;
        this.tableElement = table.getElement();
    }

    @Override
    public String getValue() {
        return title;
    }

    @Override
    public void onBrowserEvent(Context context, Element target, NativeEvent event) {
        new HeaderHelper(target, event);
    }

    private static void setCursor(Element element, Cursor cursor) {
        element.getStyle().setCursor(cursor);
    }

    interface IDragCallback {
        void dragFinished();
    }

    private static final int RESIZE_HANDLE_WIDTH = 9;

    private static NativeEvent getEventAndPreventPropagation(NativePreviewEvent event) {
        final NativeEvent nativeEvent = event.getNativeEvent();
        nativeEvent.preventDefault();
        nativeEvent.stopPropagation();
        return nativeEvent;
    }

    private static void setLine(Style style, int width, int top, int height, String color) {
        style.setPosition(Position.ABSOLUTE);
        style.setTop(top, PX);
        style.setHeight(height, PX);
        style.setWidth(width, PX);
        style.setBackgroundColor(color);
        style.setZIndex(1000);
    }

    private class HeaderHelper implements NativePreviewHandler, IDragCallback {
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        final Element mover, left, right;
        private boolean dragging;
        private final Element source;

        public HeaderHelper(Element target, NativeEvent event) {
            this.source = target;
            event.preventDefault();
            event.stopPropagation();
            mover = document.createDivElement();
            left = document.createSpanElement();
            mover.appendChild(left);
            final Style lStyle = left.getStyle();
            setCursor(left, moveCursor);
            lStyle.setPosition(Position.ABSOLUTE);
            lStyle.setTop(0, PX);
            lStyle.setBottom(0, PX);
            lStyle.setZIndex(1000);
            lStyle.setHeight(target.getOffsetHeight(), PX);
            lStyle.setTop(target.getOffsetTop(), PX);
            lStyle.setBackgroundColor(MOVE_COLOR);
            lStyle.setOpacity(GHOST_OPACITY);
            lStyle.setLeft(target.getOffsetLeft(), PX);
            lStyle.setWidth(target.getOffsetWidth() - RESIZE_HANDLE_WIDTH, PX);
            right = document.createSpanElement();
            mover.appendChild(right);
            final Style rStyle = right.getStyle();
            setCursor(right, resizeCursor);
            rStyle.setPosition(Position.ABSOLUTE);
            rStyle.setTop(0, PX);
            rStyle.setBottom(0, PX);
            rStyle.setZIndex(1000);
            rStyle.setHeight(target.getOffsetHeight(), PX);
            rStyle.setTop(target.getOffsetTop(), PX);
            rStyle.setBackgroundColor(RESIZE_COLOR);
            rStyle.setLeft(target.getOffsetLeft() + target.getOffsetWidth() - RESIZE_HANDLE_WIDTH, PX);
            rStyle.setWidth(RESIZE_HANDLE_WIDTH, PX);
            target.appendChild(mover);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            final NativeEvent nativeEvent = getEventAndPreventPropagation(event);
            final Element element = nativeEvent.getEventTarget().cast();
            final String eventType = nativeEvent.getType();
            if (!(element == left || element == right)) {
                if (!dragging && "mouseover".equals(eventType))
                    finish();
                return;
            }
            if ("mousedown".equals(eventType)) {
                if (element == right) {
                    left.removeFromParent();
                    new ColumnResizeHelper(this, source, right, nativeEvent);
                } else
                    new ColumnMoverHelper(this, source, nativeEvent);
                dragging = true;
            }
        }

        private void finish() {
            handler.removeHandler();
            mover.removeFromParent();
        }

        public void dragFinished() {
            dragging = false;
            finish();
        }
    }

    private class ColumnResizeHelper implements NativePreviewHandler {
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final DivElement resizeLine = document.createDivElement();
        private final Style resizeLineStyle = resizeLine.getStyle();
        private final Element header;
        private final IDragCallback dragCallback;
        private final Element caret;

        private ColumnResizeHelper(IDragCallback dragCallback, Element header, Element caret, NativeEvent event) {
            this.dragCallback = dragCallback;
            this.header = header;
            this.caret = caret;
            setLine(resizeLineStyle, 2, header.getAbsoluteTop() + header.getOffsetHeight(), getTableBodyHeight(), RESIZE_COLOR);
            moveLine(event.getClientX());
            tableElement.appendChild(resizeLine);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            final NativeEvent nativeEvent = getEventAndPreventPropagation(event);
            final int clientX = nativeEvent.getClientX();
            final String eventType = nativeEvent.getType();
            if ("mousemove".equals(eventType)) {
                moveLine(clientX);
            } else if ("mouseup".equals(eventType)) {
                handler.removeHandler();
                resizeLine.removeFromParent();
                dragCallback.dragFinished();
                columnResized(Math.max(clientX - header.getAbsoluteLeft(), MINIMUM_COLUMN_WIDTH));
            }
        }

        private void moveLine(final int clientX) {
            final int xPos = clientX - table.getAbsoluteLeft();
            caret.getStyle().setLeft(xPos - caret.getOffsetWidth() / 2, PX);
            resizeLineStyle.setLeft(xPos, PX);
            resizeLineStyle.setTop(header.getOffsetHeight(), PX);
        }
    }

    private class ColumnMoverHelper implements NativePreviewHandler {
        private static final int ghostLineWidth = 4;
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final DivElement ghostLine = document.createDivElement();
        private final Style ghostLineStyle = ghostLine.getStyle();
        private final DivElement ghostColumn = document.createDivElement();
        private final Style ghostColumnStyle = ghostColumn.getStyle();
        private final int columnWidth;
        private final int[] columnXPositions;
        private final IDragCallback dragCallback;
        private int fromIndex = -1;
        private int toIndex;

        private ColumnMoverHelper(IDragCallback dragCallback, Element target, NativeEvent event) {
            this.dragCallback = dragCallback;
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
            final NativeEvent nativeEvent = getEventAndPreventPropagation(event);
            final String eventType = nativeEvent.getType();
            if ("mousemove".equals(eventType)) {
                moveColumn(nativeEvent.getClientX());
            } else if ("mouseup".equals(eventType)) {
                handler.removeHandler();
                ghostColumn.removeFromParent();
                ghostLine.removeFromParent();
                if (fromIndex != toIndex)
                    columnMoved(fromIndex, toIndex);
                dragCallback.dragFinished();
            }
        }

        private void moveColumn(final int clientX) {
            final int pointer = clientX - columnWidth / 2;
            ghostColumnStyle.setLeft(pointer - table.getAbsoluteLeft(), PX);
            for (int i = 0; i < columnXPositions.length - 1; ++i) {
                if (clientX < columnXPositions[i + 1]) {
                    final int adjustedIndex = i > fromIndex ? i + 1 : i;
                    int lineXPos = columnXPositions[adjustedIndex] - table.getAbsoluteLeft();
                    if (adjustedIndex == columnXPositions.length - 1) //last columns
                        lineXPos -= ghostLineWidth;
                    else if (adjustedIndex > 0)
                        lineXPos -= ghostLineWidth / 2;
                    ghostLineStyle.setLeft(lineXPos, PX);
                    toIndex = i;
                    break;
                }
            }
        }
    }

    private static class HeaderCell extends AbstractCell<String> {
        public HeaderCell() {
            super("mousemove");
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
