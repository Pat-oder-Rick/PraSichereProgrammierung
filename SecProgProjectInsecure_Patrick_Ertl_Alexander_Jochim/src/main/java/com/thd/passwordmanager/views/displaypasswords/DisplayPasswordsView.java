package com.thd.passwordmanager.views.displaypasswords;

import java.util.Iterator;
import java.util.Set;

import com.thd.passwordmanager.DBController;
import com.thd.passwordmanager.EntryDTO;
import com.thd.passwordmanager.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@PageTitle("display passwords")
@Route(value = "display-passwords", layout = MainLayout.class)
public class DisplayPasswordsView extends VerticalLayout {

	private TextField platformField = new TextField();
	private TextField usernameField = new TextField();
	private PasswordField passwordField = new PasswordField();
	private Button addEntryButton = new Button("Add entry");
	private Button deleteEntry = new Button("delete entrys");
	private HorizontalLayout layoutEntry = new HorizontalLayout();
	private HorizontalLayout layoutTableConfig = new HorizontalLayout();
	private H2 header = new H2("Not logged in");
	private Grid<EntryDTO> grid = new Grid<>(EntryDTO.class, false);

	public DisplayPasswordsView() {

		setSpacing(false);
		setSizeFull();
		setJustifyContentMode(JustifyContentMode.CENTER);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		getStyle().set("text-align", "center");
		if (DBController.getState() == true) {
			printLoggedIn();
			printGrid();
		} else {
			printLoggedOut();
		}

		addEntryButton.addClickListener(e -> {
			if (platformField.getValue() == null || usernameField.getValue() == null
					|| passwordField.getValue() == null) {

			} else if (DBController.addEntry(platformField.getValue(), DBController.currentUserID,
					usernameField.getValue(), passwordField.getValue())) {
				clearFields();
				grid.setItems(DBController.getEntryset());
			}
		});

		deleteEntry.addClickListener(e -> {
			grid.addSelectionListener(event -> {
				EntryDTO entry;
				Set<EntryDTO> selected = event.getAllSelectedItems();
				Iterator<EntryDTO> itr = selected.iterator();
				while (itr.hasNext()) {
					entry = itr.next();
					DBController.deleteEntry(entry.getPwid());
				}
			});
			grid.setItems(DBController.getEntryset());
		});
	}

	public void printLoggedOut() {
		add(header);
	}

	public void printLoggedIn() {
		header.setText("Hello " + DBController.getUsername() + "!");
		add(header);
		layoutEntry.add(platformField);
		layoutEntry.add(usernameField);
		layoutEntry.add(passwordField);
		add(layoutEntry);
		add(layoutTableConfig);
		layoutTableConfig.add(addEntryButton);
		layoutTableConfig.add(deleteEntry);
	}

	public void printGrid() {
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		grid.addColumn(EntryDTO::getPlatform).setHeader("platform");
		grid.addColumn(EntryDTO::getUsername).setHeader("username/email");
		grid.addColumn(EntryDTO::getPassword).setHeader("password");
		grid.setItems(DBController.getEntryset());
		add(grid);
	}

	public void clearFields() {
		platformField.clear();
		usernameField.clear();
		passwordField.clear();
	}

	public void prepareFields() {
		platformField.setLabel("Platform");
		usernameField.setLabel("Email/username");
		passwordField.setLabel("password");
		usernameField.setHelperText("at least 8 characters.");
		platformField.setHelperText("at least 8 characters.");
		passwordField.setHelperText("at least 8 characters.");
	}
}
