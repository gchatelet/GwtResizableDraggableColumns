package fr.mikrosimage.gwt.client;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.DockLayoutPanel;
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

    /**
     * The list of data to display.
     */
    private static final List<Contact> CONTACTS = Arrays.asList(new Contact("John", new Date(80, 4, 12), "123 Fourth Avenue"), new Contact("Joe",
            new Date(85, 2, 22), "22 Lance Ln"), new Contact("George", new Date(46, 6, 6), "1600 Pennsylvania Avenue"));

    public void onModuleLoad() {
        // Create a CellTable.
        //        CellTable<Contact> table = new CellTable<Contact>();
        //        // Set the total row count. This isn't strictly necessary, but it affects
        //        // paging calculations, so its good habit to keep the row count up to date.
        //        table.setRowCount(CONTACTS.size(), true);
        //        // Push the data into the widget.
        //        table.setRowData(0, CONTACTS);
        ResizableDataGrid<Contact> table = new ResizableDataGrid<Contact>();
        final ListDataProvider<Contact> dataProvider = new ListDataProvider<Contact>(CONTACTS);
        dataProvider.addDataDisplay(table);
        table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        // Add a text column to show the name.
        TextColumn<Contact> nameColumn = new TextColumn<Contact>() {
            @Override
            public String getValue(Contact object) {
                return object.name;
            }
        };
        table.addColumn(nameColumn, table.new DataGridResizableHeader("Name", nameColumn));
        // Add a date column to show the birthday.
        DateCell dateCell = new DateCell();
        Column<Contact, Date> dateColumn = new Column<Contact, Date>(dateCell) {
            @Override
            public Date getValue(Contact object) {
                return object.birthday;
            }
        };
        table.addColumn(dateColumn, table.new DataGridResizableHeader("Birthday", dateColumn));
        // Add a text column to show the address.
        TextColumn<Contact> addressColumn = new TextColumn<Contact>() {
            @Override
            public String getValue(Contact object) {
                return object.address;
            }
        };
        table.addColumn(addressColumn, table.new DataGridResizableHeader("Address", addressColumn));
        //
        //
        // Add it to the root panel.
        final DockLayoutPanel dockLayoutPanel = new DockLayoutPanel(Unit.PX);
        dockLayoutPanel.addNorth(new Label(), 50);
        dockLayoutPanel.addEast(new Label(), 50);
        dockLayoutPanel.addSouth(new Label(), 100);
        dockLayoutPanel.addWest(new Label(), 100);
        dockLayoutPanel.add(table);
        RootLayoutPanel.get().add(dockLayoutPanel);
        //        RootLayoutPanel.get().add(table);
    }
}