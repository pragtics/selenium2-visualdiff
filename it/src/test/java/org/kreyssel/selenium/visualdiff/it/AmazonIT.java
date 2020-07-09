package org.kreyssel.selenium.visualdiff.it;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kreyssel.selenium.visualdiff.core.junit4.TakesScreenshotRule;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * SimpleSeleniumIT.
 */
public class AmazonIT {

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
		driver.get("http://www.amazon.com");

		screenshot.takeScreenshot(driver);
	}

	@Test
	public void searchBooks() throws Exception {
		driver.get("http://www.amazon.com");

		screenshot.takeScreenshot("startPage", driver);

		driver.findElementById("twotabsearchtextbox").sendKeys("selenium3");

		screenshot.takeScreenshot("afterInput", driver);

		driver.findElementByClassName("nav-search-submit").findElement(By.tagName("input")).click();

		wait.until(webDriver -> {
			System.out.println("searching ...");
			return webDriver.findElement(By.className("s-result-list")) != null;
		});

		screenshot.takeScreenshot("afterSearch", driver);
	}

	private RemoteWebDriver createDriver() {
		if (SystemUtils.IS_OS_WINDOWS)
			return new InternetExplorerDriver();

		WebDriverManager.firefoxdriver().setup();
		return new FirefoxDriver();
	}
}
