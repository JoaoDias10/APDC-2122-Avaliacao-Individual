package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {

	public static final String ADDRESS = "%s, %s, %s";

	private String username;
	private String password;
	private String newPassword;
	private String confirmation;
	private String email;
	private String name;
	private String profile;
	private String landline;
	private String mobilePhone;
	private String address;
	private String nif;
	private String role;
	private String status;

	private String userToBeRemoved;

	private AuthToken authToken;

	public RegisterData() {
	}// Nothing to be done here (could be omitted)

	public RegisterData(String username, String password, String newPassword, String confirmation, String email,
			String name, String profile, String landline, String mobilePhone, String address, String NIF, String role,
			String status) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.newPassword = newPassword;
		this.email = email;
		this.name = name;
		this.profile = profile;
		this.landline = landline;
		this.mobilePhone = mobilePhone;
		this.address = address;
		this.nif = NIF;
		this.role = role;
		this.status = status;
	}

	public boolean validRegistration() {
		if (username == null || password == null || confirmation == null || email == null || name == null)
			return false;

		if (username.length() == 0 || password.length() < 10 || confirmation.length() == 0 || email.length() == 0
				|| !email.matches("(.*)@(.*).(.*)") || name.length() == 0)
			return false;

		return password.equals(confirmation);
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public String getConfirmation() {
		return confirmation;
	}

	public String getEmail() {
		return email;
	}

	public String getName() {
		return name;
	}

	public String getProfile() {
		return profile;
	}

	public String getLandline() {
		return landline;
	}

	public String getMobilePhone() {
		return mobilePhone;
	}

	public String getAddress() {
		return address;
	}

	public String getNIF() {
		return nif;
	}

	public String getRole() {
		return role;
	}

	public String getStatus() {
		return status;
	}

	public String getUserToBeRemoved() {
		return userToBeRemoved;
	}

	public AuthToken getAuthToken() {
		return authToken;
	}

	public boolean validProfile() {
		return profile.equals("P??blico") || profile.equals("Privado");
	}

	public boolean validStatus() {
		return status.equals("INATIVO") || status.equals("ATIVO");
	}

	public boolean validRole() {
		return role.equals("SU") || role.equals("GS") || role.equals("GBO") || role.equals("USER");
	}

	public boolean validStrings() {
		return landline.length() == 9 && mobilePhone.length() == 9 && nif.length() == 9;
	}

	public boolean validNewPassword() {
		return !newPassword.equals(password) && newPassword.equals(confirmation);
	}
}
