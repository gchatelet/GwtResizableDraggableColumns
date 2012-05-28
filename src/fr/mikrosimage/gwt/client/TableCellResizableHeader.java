package fr.mikrosimage.gwt.client;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;

public class TableCellResizableHeader<T> extends ResizableHeader<T> {
    private final CellTable<T> table;

    public TableCellResizableHeader(String title, CellTable<T> table, Column<T, ?> column) {
        super(title, table, column);
        this.table = table;
    }

    @Override
    protected int getTableBodyHeight() {
        return table.getBodyHeight();
    }
}
