package org.oxtrust.qa.pages.users;

import java.util.List;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.oxtrust.qa.pages.AbstractPage;

public class UserManagePage extends AbstractPage {
	private List<WebElement> inputs;
	private WebElement tableBody;
	private WebElement foundUser;

	public void searchUser(String pattern) {
		WebElement searchBox = webDriver.findElement(By.className("searchArea"));
		WebElement searchSpanBox = searchBox.findElement(By.tagName("span"));
		inputs = searchSpanBox.findElements(By.tagName("input"));
		Assert.assertTrue(inputs.size() == 2);
		inputs.get(0).sendKeys(pattern);
		inputs.get(1).click();

	}

	public boolean userExistList(String username) {
		try {
			tableBody = webDriver.findElement(By.id("personsFormId:personsListId")).findElements(By.tagName("tbody"))
					.get(0);
		} catch (Exception e) {
			System.out.println("############## FAILED STATE");
			return false;
		}
		List<WebElement> trs = tableBody.findElements(By.tagName("tr"));
		boolean found = false;
		for (WebElement element : trs) {
			if (element.getText().contains(username)) {
				found = true;
				foundUser = element;
				break;
			}
		}
		return found;
	}

	public void assertUserWithExist(String userName) {
		Assert.assertTrue(userExistList(userName));
	}

	public void startUserUpdate() {
		foundUser.findElements(By.tagName("td")).get(0).click();
	}

	public void assertUserNotWithExist(String userName) {
		Assert.assertFalse(userExistList(userName));
	}

}
