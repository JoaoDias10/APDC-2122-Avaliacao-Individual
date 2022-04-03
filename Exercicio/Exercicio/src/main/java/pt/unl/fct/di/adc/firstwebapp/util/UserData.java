package pt.unl.fct.di.adc.firstwebapp.util;

public class UserData {
	private String username;
	private String password;
	private String email;
	private String name;
	private String profile;
	private String landline;
	private String mobilePhone;
	private String address;
	private String NIF;
	private String role;
	private String status;

	public UserData(String username, String password, String email, String name, String profile, String landline,
			String mobilePhone, String address, String NIF, String role, String status) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.name = name;
		this.profile = profile;
		this.landline = landline;
		this.mobilePhone = mobilePhone;
		this.address = address;
		this.NIF = NIF;
		this.role = role;
		this.status = status;
	}
}
