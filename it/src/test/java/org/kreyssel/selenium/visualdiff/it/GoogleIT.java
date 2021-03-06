package org.kreyssel.selenium.visualdiff.it;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kreyssel.selenium.visualdiff.core.junit4.TakesScreenshotRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * SimpleSeleniumIT.
 */
public class GoogleIT {

	@Rule
	public TakesScreenshotRule screenshot = new TakesScreenshotRule();

	RemoteWebDriver driver;
	WebDriverWait wait;

	@Before
	public void init() {
		driver = createDriver();
		wait = new WebDriverWait(driver, 30);
	}

	@After
	public void destroy() {
		driver.close();
	}

	@Test
	public void startPage() throws Exception {
		driver.get("http://www.google.com");

		screenshot.takeScreenshot(driver);
	}

	@Test
	public void searchWeb() throws Exception {
		driver.get("http://www.google.com/?q=news+2011");

		screenshot.takeScreenshot("edit", driver);

		driver.findElementByName("btnG").click();

		wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(final WebDriver webDriver) {
				System.out.println("searching ...");
				return webDriver.findElement(By.id("resultStats")) != null;
			}
		});

		screenshot.takeScreenshot("afterSubmit", driver);
	}

	@Test
	public void searchImages() throws Exception {
		driver.get("http://www.google.com/");

		screenshot.takeScreenshot("open", driver);

		driver.findElementById("gb_2").click();

		wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(final WebDriver webDriver) {
				System.out.println("switch to images ...");
				return webDriver.findElement(By.name("q")) != null;
			}
		});

		screenshot.takeScreenshot("afterSwitchToImages", driver);

		driver.findElementByName("q").sendKeys("selenium-2\n");

		wait.until(new ExpectedCondition<Boolean>() {
			public Boolean apply(final WebDriver webDriver) {
				System.out.println("searching ...");
				return webDriver.findElement(By.id("resultStats")) != null;
			}
		});

		screenshot.takeScreenshot("searchResult", driver);
	}

	private RemoteWebDriver createDriver() {
		if (SystemUtils.IS_OS_WINDOWS)
			return new InternetExplorerDriver();

		return new FirefoxDriver();
	}
}
