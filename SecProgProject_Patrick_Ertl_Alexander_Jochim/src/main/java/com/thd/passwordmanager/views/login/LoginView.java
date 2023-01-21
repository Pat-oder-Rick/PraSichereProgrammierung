package com.thd.passwordmanager.views.login;

import java.io.UnsupportedEncodingException;

import com.thd.passwordmanager.DBController;
import com.thd.passwordmanager.PasswordFieldSecure;
import com.thd.passwordmanager.TextFieldSecure;
import com.thd.passwordmanager.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@PageTitle("login")
@Route(value = "login", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)

public class LoginView extends VerticalLayout {

	private Button createAccountButton = new Button("create Account");
	private Button logoutButton = new Button("Log out");
	private Button loginButton = new Button("Log in");
	private Button registerButton = new Button("register");
	private Button backButton = new Button("back");
	private HorizontalLayout layout = new HorizontalLayout();
	private HorizontalLayout logoutLayout = new HorizontalLayout();
	private H2 header = new H2("login");
	private PasswordFieldSecure passwordField = new PasswordFieldSecure();
	private TextFieldSecure usernameField = new TextFieldSecure();

	public LoginView() {
		setSpacing(false);
		setSizeFull();
		setJustifyContentMode(JustifyContentMode.CENTER);
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		getStyle().set("text-align", "center");
		add(header);
		prepareFields();
		prepareButtons();
		if (DBController.getState() == true) {
			printLoggedIn();
		} else {
			printLogin();
			try {
				DBController.connect();
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			}
		}

		logoutButton.addClickListener(e -> {
			DBController.setState(false);
			printLogin();
		});

		backButton.addClickListener(e -> {
			printLogin();
		});

		loginButton.addClickListener(e -> login());

		registerButton.addClickListener(e -> printRegister());

		createAccountButton.addClickListener(e -> {
			try {
				createAccount();
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
		});
	}

	/**
	 * 
	 */
	private void clearFields() {
		usernameField.setValue("");
		passwordField.setValue("");
	}

	/**
	 * 
	 */
	public void printLogin() {
		header.setText("login");
		layout.remove(createAccountButton, backButton);
		clearFields();
		removeAll();
		add(header, usernameField, passwordField);
		layout.add(loginButton, registerButton);
		add(layout);
	}

	/**
	 * 
	 */
	public void printRegister() {
		layout.remove(registerButton);
		layout.remove(loginButton);
		layout.add(createAccountButton);
		layout.add(backButton);
		header.setText("register");
		clearFields();
	}

	/**
	 * 
	 */
	public void printLoggedIn() {
		clearFields();
		removeAll();
		logoutLayout.setSizeFull();
		logoutLayout.add(logoutButton);
		add(logoutLayout);
		header.setText("Already logged in");
	}

	/**
	 * 
	 */
	private void prepareFields() {
		usernameField.setClearButtonVisible(true);
		usernameField.setLabel("Username");
		usernameField.setValue("");
		usernameField.setHelperText("at least 8 characters.");
		add(usernameField);
		passwordField.setLabel("Password");
		passwordField.setHelperText("A password must be at least 8 characters. ");
		add(passwordField);
	}

	/**
	 * 
	 */
	private void prepareButtons() {
		loginButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
		createAccountButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
		logoutButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
	}

	/**
	 * 
	 */
	public void login() {
		if (usernameField.getValue() == null || passwordField.getValue() == null) {
			printErrorMessage();
		} else {
			if (DBController.login(usernameField.getValue(), passwordField.getValue().toCharArray())) {
				printLoggedIn();
			} else {
				printErrorMessage();
			}
		}
	}

	/**
	 * @throws UnsupportedEncodingException
	 */
	public void createAccount() throws UnsupportedEncodingException {
		if (usernameField.getUniqueUsername() == null || passwordField.getValue() == null) {
			printErrorMessage();
		} else if (DBController.createAccount(usernameField.getValue(), passwordField.getValue().toCharArray())) {

			printLogin();
		} else {
			printErrorMessage();
		}
	}

	/**
	 * @return String
	 */
	public String getErrorMessage() {
		switch (DBController.getErrorMessage()) {
		case "length":
			return "password too long or short";
		case "invalidInput":
			return "invalid input";
		case "duplicateUsername":
			return "username already in use";
		}
		return null;
	}

	/**
	 * 
	 */
	public void printErrorMessage() {
		new Notification(getErrorMessage(), 3000).open();
	}
}
