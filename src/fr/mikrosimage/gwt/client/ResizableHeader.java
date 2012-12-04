package fr.mikrosimage.gwt.client;

import static com.google.gwt.dom.client.Style.Unit.PX;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.SpanElement;
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
    //TODO Have following strings localized from separate properties file 
    private static final String MOVE = "Move";
    private static final String RESIZE = "Resize";
    private static final String MOVE_tt = "Click and drag to move column";
    private static final String RESIZE_tt = "Click and drag to resize column";
    private static final Style.Cursor moveCursor = Cursor.MOVE;
    private static final Style.Cursor resizeCursor = Cursor.COL_RESIZE;
    private static final String RESIZE_COLOR = "#A49AED";
    private static final String MOVE_COLOR = "gray";
    private static final String FOREGROUND_COLOR = "white";
    private static final double GHOST_OPACITY = .3;
    private static final int MINIMUM_COLUMN_WIDTH = 30;
    private final String title;
    private final Document document = Document.get();
    private final AbstractCellTable<T> table;
    private final Element tableElement;
    private HeaderHelper current;
    protected final Column<T, ?> column;
    private final String moveStyle;
    private final String resizeStyle;
    private final String moveToolTip;
    private final String resizeToolTip;   

    public ResizableHeader(String title, AbstractCellTable<T> table, Column<T, ?> column) {
        this(title, table, column, null, null, null, null);
    }

    public ResizableHeader(String title, AbstractCellTable<T> table, Column<T, ?> column,
                           String moveStyle, String resizeStyle, String moveToolTip, String resizeToolTip) {
        super(new HeaderCell());
        if (title == null || table == null || column == null)
            throw new NullPointerException();
        this.title = title;
        this.column = column;
        this.table = table;
        this.tableElement = table.getElement();
        this.moveStyle = moveStyle;
        this.resizeStyle = resizeStyle;
        this.moveToolTip = moveToolTip;
        this.resizeToolTip = resizeToolTip;
    }

    @Override
    public String getValue() {
        return title;
    }

    @Override
    public void onBrowserEvent(Context context, Element target, NativeEvent event) {
        if (current == null)
            current = new HeaderHelper(target, event);
    }

    interface IDragCallback {
        void dragFinished();
    }

    private static final int RESIZE_HANDLE_WIDTH = 34;

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
    }

    private class HeaderHelper implements NativePreviewHandler, IDragCallback {
        private final HandlerRegistration handler = Event.addNativePreviewHandler(this);
        private final Element source;
        private boolean dragging;
        final Element mover, left, right;

        public HeaderHelper(Element target, NativeEvent event) {
            this.source = target;
            System.out.println(source.getOffsetTop() + " " +source.getAbsoluteTop());
            event.preventDefault();
            event.stopPropagation();
            mover = document.createDivElement();
            final int leftBound = target.getOffsetLeft() + target.getOffsetWidth();
        if (moveStyle != null) {
                left = createSpanElement(moveStyle, moveToolTip, leftBound - 2 * RESIZE_HANDLE_WIDTH);
            }else {
                left = createSpanElement(MOVE, MOVE_tt, MOVE_COLOR, moveCursor, leftBound - 2 * RESIZE_HANDLE_WIDTH);
            }
	    if (resizeStyle != null) {
                right = createSpanElement(resizeStyle, resizeToolTip, leftBound - RESIZE_HANDLE_WIDTH);
            }else {
                right = createSpanElement(RESIZE, RESIZE_tt, RESIZE_COLOR, resizeCursor, leftBound - RESIZE_HANDLE_WIDTH);
            }
            mover.appendChild(left);
            mover.appendChild(right);
            source.appendChild(mover);
        }
        
        private SpanElement createSpanElement(String styleClassName, String title, double left){
            final SpanElement span = document.createSpanElement();
            span.setClassName(styleClassName);
            if (title != null) {
                span.setTitle(title);
            }
            final Style style = span.getStyle();
            style.setPosition(Position.ABSOLUTE);
            style.setBottom(0, PX);
            style.setHeight(source.getOffsetHeight(), PX);
            style.setTop(source.getOffsetTop(), PX);
            style.setWidth(RESIZE_HANDLE_WIDTH, PX);
            style.setLeft(left, PX);
            return span;
        }

        private SpanElement createSpanElement(String innerText, String title, String backgroundColor, Cursor cursor, double left){
            final SpanElement span = document.createSpanElement();
            span.setInnerText(innerText);
            span.setAttribute("title", title);
            final Style style = span.getStyle();
            style.setCursor(cursor);
            style.setPosition(Position.ABSOLUTE);
            style.setBottom(0, PX);
            style.setHeight(source.getOffsetHeight(), PX);
            style.setTop(source.getOffsetTop(), PX);
            style.setColor(FOREGROUND_COLOR);
            style.setWidth(RESIZE_HANDLE_WIDTH, PX);
            style.setLeft(left, PX);
            style.setBackgroundColor(backgroundColor);
            return span;
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            final NativeEvent natEvent = event.getNativeEvent();
            final Element element = natEvent.getEventTarget().cast();
            final String eventType = natEvent.getType();
            if (!(element == left || element == right)) {
                if ("mousedown".equals(eventType)) {
                    //No need to do anything, the event will be passed on to the column sort handler
                } else if (!dragging && "mouseover".equals(eventType)) {
                    cleanUp();
                }
                return;
            }
            final NativeEvent nativeEvent = getEventAndPreventPropagation(event);
            if ("mousedown".equals(eventType)) {
                if (element == right) {
                    left.removeFromParent();
                    new ColumnResizeHelper(this, source, right, nativeEvent);
                } else
                    new ColumnMoverHelper(this, source, nativeEvent);
                dragging = true;
            }
        }

        private void cleanUp() {
            handler.removeHandler();
            mover.removeFromParent();
            current = null;
        }

        public void dragFinished() {
            dragging = false;
            cleanUp();
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
            Element tr = target.getParentElement();
            while (!tr.getNodeName().equals("TR")) {
            	tr = tr.getParentElement();
            }
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