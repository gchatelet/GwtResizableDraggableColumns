package fr.mikrosimage.gwt.client;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.view.client.ListDataProvider;

public class GwtResizableDraggableColumns implements EntryPoint {
    /**
     * A simple data type that represents a contact.
     */
    private static class Contact {
        private final String address;
        private final Date birthday;
        private final String name;

        public Contact(String name, Date birthday, String address) {
            this.name = name;
            this.birthday = birthday;
            this.address = address;
        }
    }

    @SuppressWarnings("deprecation")
    private static Date date(int year, int month, int date) {
        return new Date(year, month, date);
    }

    private static final List<Contact> CONTACTS = Arrays.asList(//
            new Contact("John", date(1980, 4, 12), "123 Fourth Avenue"), //
            new Contact("Joe", date(1985, 2, 22), "22 Lance Ln"), //
            new Contact("George", date(1946, 6, 6), "1600 Pennsylvania Avenue"));
    
    private static final String COLUMN_NAME = "Name";

    private static TextColumn<Contact> buildNameColumn() {
        return new TextColumn<Contact>() {
            @Override
            public String getValue(Contact object) {
                return object.name;
            }
        };
    }
    private static final Comparator<Contact> nameComparator = new Comparator<Contact>() {
        public int compare(Contact o1, Contact o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 != null) {
                return (o2 != null) ? o1.name.compareTo(o2.name) : 1;
            }
            return -1;
        }
    };

    private static final String COLUMN_BIRTHDAY = "Birthday";

    private static Column<Contact, Date> buildDateColumn() {
        return new Column<Contact, Date>(new DateCell()) {
            @Override
            public Date getValue(Contact object) {
                return object.birthday;
            }
        };
    }

    private static final String COLUMN_ADDRESS = "Address";

    private static TextColumn<Contact> buildAddressColumn() {
        return new TextColumn<Contact>() {
            @Override
            public String getValue(Contact object) {
                return object.address;
            }
        };
    }

    private static DataGrid<Contact> createDataGrid() {
        // columns
        final Column<Contact, String> nameColumn = buildNameColumn();
        final Column<Contact, Date> dateColumn = buildDateColumn();
        final Column<Contact, String> addressColumn = buildAddressColumn();
        // table
        final ResizableDataGrid<Contact> table = new ResizableDataGrid<Contact>();
        table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        table.addColumn(nameColumn, table.new DataGridResizableHeader(COLUMN_NAME, nameColumn));
        table.addColumn(dateColumn, table.new DataGridResizableHeader(COLUMN_BIRTHDAY, dateColumn));
        table.addColumn(addressColumn, table.new DataGridResizableHeader(COLUMN_ADDRESS, addressColumn));
        // data
        final ListDataProvider<Contact> dataProvider = new ListDataProvider<Contact>(CONTACTS);
        dataProvider.addDataDisplay(table);
        // sorting
        final ListHandler<Contact> columnSortHandler = new ListHandler<Contact>(dataProvider.getList());
        columnSortHandler.setComparator(nameColumn, nameComparator);
        table.addColumnSortHandler(columnSortHandler);
        nameColumn.setSortable(true);
        table.setSize("640px", "200px");
        return table;
    }

    private static CellTable<Contact> createCellTable() {
        // columns
        final Column<Contact, String> nameColumn = buildNameColumn();
        final Column<Contact, Date> dateColumn = buildDateColumn();
        final Column<Contact, String> addressColumn = buildAddressColumn();
        // table
        final CellTable<Contact> table = new CellTable<Contact>();
        table.addColumn(nameColumn, new TableCellResizableHeader<Contact>(COLUMN_NAME, table, nameColumn));
        table.addColumn(dateColumn, new TableCellResizableHeader<Contact>(COLUMN_BIRTHDAY, table, dateColumn));
        table.addColumn(addressColumn, new TableCellResizableHeader<Contact>(COLUMN_ADDRESS, table, addressColumn));
        // data
        table.setRowData(0, CONTACTS);
        table.setRowCount(CONTACTS.size(), true);
        return table;
    }

    public void onModuleLoad() {
        final DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Unit.PX);
        dockLayoutPanel.addNorth(new Label("north placeholder to test layout issues"), 50);
        dockLayoutPanel.addEast(new Label("east"), 50);
        dockLayoutPanel.addSouth(new Label("south"), 100);
        dockLayoutPanel.addWest(new Label("west"), 100);
        final FlowPanel panel = new FlowPanel();
        panel.add(new Label("data grid (name is sortable)"));
        panel.add(createDataGrid());
        panel.add(new Label("cell table"));
        panel.add(createCellTable());
        dockLayoutPanel.add(panel);
        RootLayoutPanel.get().add(dockLayoutPanel);
    }
}