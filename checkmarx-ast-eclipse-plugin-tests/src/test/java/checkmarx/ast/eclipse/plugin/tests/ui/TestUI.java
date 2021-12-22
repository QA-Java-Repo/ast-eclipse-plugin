package checkmarx.ast.eclipse.plugin.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.checkmarx.eclipse.enums.ActionName;
import com.checkmarx.eclipse.enums.Severity;
import com.checkmarx.eclipse.utils.PluginConstants;
import com.checkmarx.eclipse.views.actions.ToolBarActions;

import checkmarx.ast.eclipse.plugin.tests.common.Environment;

@RunWith(SWTBotJunit4ClassRunner.class)
public class TestUI extends BaseUITest {
	
	private static final String ERROR_SERVER_URL_NOT_SET = "Checkmarx server URL is not set";

	private static final String INFO_SUCCESSFUL_CONNECTION = "Successfully authenticated to AST server!";
	
	private static final String ASSERT_FILTER_ACTIONS_IN_TOOLBAR = "All filter actions must be in the tool bar";
	private static final String ASSERT_GROUP_BY_ACTIONS_IN_TOOLBAR = "All group by actions must be in the tool bar";
	private static final String ASSERT_TREE_CONSTAIN_HIGH_MEDIUM = "Results must contain results grouped by High and Medium";
	private static final String ASSERT_TREE_CONSTAIN_HIGH_MEDIUM_LOW = "Results must contain results grouped by High, Medium and Low";
	private static final String ASSERT_TREE_CONSTAIN_HIGH_MEDIUM_LOW_INFO = "Results must contain results grouped by High, Medium, Low and Info";
	private static final String ASSERT_TREE_WITH_NO_ISSUES = "The tree mustn't have results once we are grouping by severity and no severity is selected";
	private static final String ASSERT_GROUP_BY_QUERY_NAME = "Parent name must be equals to child name once it is grouped by query name";
	private static final String ASSERT_NO_CHINDREN = "One group by severity and group by query name are not selected, this node shouldn't have children";
	private static final String ASSERT_GROUP_BY_SEVERITY_NOT_SELECTED = "Engine child should not be HIGH, MEDIUM, LOW or INFO once the group by severity is not enabled";
	private static final String ASSERT_CREDENTIALS_PANEL = "The  credentials panel must appear once Checkmarx credentials are not defined";
	
	private static boolean _cxSettingsDefined = false;

	@Test
	public void testSuccessfulConnetion() throws TimeoutException {
		testSuccessfulConnection(false);		
	}

	@Test
	public void testAddCheckmarxASTPlugin() throws TimeoutException {
		// Add Checkmarx plugin to the eclipse view
		addCheckmarxPlugin(false);

		// Assert that active view is the Checkmarx AST Scan
		assertTrue("Active view must be the Checkmarx AST Scan", _bot.activeView().getTitle().equals(VIEW_CHECKMARX_AST_SCAN));
		
		preventWidgetWasNullInCIEnvironment();
		
		assertTrue(ASSERT_CREDENTIALS_PANEL, _bot.button(PluginConstants.BTN_OPEN_SETTINGS) != null);
		
		// Close Checkmarx AST Scan view
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).close();
	}

	@Test
	public void testMissingSetCheckmarxServerUrl() throws TimeoutException {
		// Test Connection
		testSuccessfulConnection(false);

		// Add Checkmarx AST Plugin
		addCheckmarxPlugin(false);
		
		// Clear Checkmarx credentials to expect missing Server Url
		clearCheckmarxCredentials();

		// After a sleep the missing Server Url message must be displayed
		assertEquals(ERROR_SERVER_URL_NOT_SET, _bot.tree().cell(0, 0));
		
		// Close Checkmarx AST Scan view
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).close();
	}

	/**
	 * Test UI End-to-End
	 * 
	 * 		-> Set Checkmarx credentials and test connection 
	 * 		-> Add Checkmarx plugin to the show view perspective 
	 * 		-> Type and assert invalid scan id 
	 * 		-> Type and assert valid scan id 
	 * 		-> Expand scan results
	 * 
	 * @throws TimeoutException
	 */
	@Test
	public void testEnd2End() throws TimeoutException {
		// Set credentials, test connection and add checkmarx plugin
		setUpCheckmarxPlugin(false);
				
		String firstNodeName = _bot.tree().cell(0, 0);
		String secondNodeName = _bot.tree().getTreeItem(firstNodeName).expand().getNode(0).getText();
		String thirdNodeName = _bot.tree().getTreeItem(firstNodeName).expand().getNode(0).expand().getNode(0).getText();
		
		// Expand nodes until the first vulnerability
		_bot.tree().expandNode(firstNodeName).expandNode(secondNodeName).expandNode(thirdNodeName).getNode(0).select();
				
		// Close Checkmarx AST Scan view
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).close();
	}
	
	@Test
	public void testFilterButtonsAndGroupByActionsInToolBar() throws TimeoutException {
		// Test Connection
		testSuccessfulConnection(false);

		// Add Checkmarx AST Plugin
		addCheckmarxPlugin(false);
				
		List<SWTBotToolbarButton> toolbarButtons = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).getToolbarButtons();
		List<String> toolBarButtonsNames = toolbarButtons.stream().map(btn -> btn.getToolTipText().toUpperCase()).collect(Collectors.toList());
		List<String> filterActions = Arrays.asList(ActionName.HIGH.name(), ActionName.MEDIUM.name(), ActionName.LOW.name(), ActionName.INFO.name());
		
		// Assert all filter actions are present in the tool bar
		assertTrue(ASSERT_FILTER_ACTIONS_IN_TOOLBAR, toolBarButtonsNames.containsAll(filterActions));	
		
		List<String> groupByActions = Arrays.asList(ToolBarActions.GROUP_BY_SEVERITY, ToolBarActions.GROUP_BY_QUERY_NAME);
		List<String> toolBarGroupByActions = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).viewMenu().menu(ToolBarActions.MENU_GROUP_BY).menuItems();
		
		// Assert all group by actions are present in the tool bar
		assertTrue(ASSERT_GROUP_BY_ACTIONS_IN_TOOLBAR, toolBarGroupByActions.containsAll(groupByActions));	
		
		// Close Checkmarx AST Scan view
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).close();
	}
	
	@Test
	public void testFilteringAndGroupingResults() throws TimeoutException {
		// Set credentials, test connection and add checkmarx plugin
		setUpCheckmarxPlugin(true);
		
		ArrayList<String> currentActiveFilters = new ArrayList<>(Arrays.asList(Severity.HIGH.name(), Severity.MEDIUM.name()));	
				
		// Checks that tree contains High and Medium results
		assertTrue(ASSERT_TREE_CONSTAIN_HIGH_MEDIUM, expandTreeUntilFirstEngineAndGetCurrentSeverities().containsAll(currentActiveFilters));			
		
		// Click to include Low severity
		clickSeverityFilter(ActionName.LOW.name());
		currentActiveFilters.add(Severity.LOW.name());
		
		// Checks that tree contains High, Medium and Low results
		assertTrue(ASSERT_TREE_CONSTAIN_HIGH_MEDIUM_LOW, expandTreeUntilFirstEngineAndGetCurrentSeverities().containsAll(currentActiveFilters));	
		
		// Click to include Info severity
		clickSeverityFilter(ActionName.INFO.name());
		currentActiveFilters.add(Severity.INFO.name());
		
		// Checks that tree contains High, Medium, Low and Info results
		assertTrue(ASSERT_TREE_CONSTAIN_HIGH_MEDIUM_LOW_INFO, expandTreeUntilFirstEngineAndGetCurrentSeverities().containsAll(currentActiveFilters));	
		
		// Get all filter buttons individually
		SWTBotToolbarButton filterHighBtn = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).getToolbarButtons().stream().filter(btn -> btn.getToolTipText().toUpperCase().equals(ActionName.HIGH.name())).findFirst().get();
		SWTBotToolbarButton filterMediumBtn = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).getToolbarButtons().stream().filter(btn -> btn.getToolTipText().toUpperCase().equals(ActionName.MEDIUM.name())).findFirst().get();
		SWTBotToolbarButton filterLowBtn = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).getToolbarButtons().stream().filter(btn -> btn.getToolTipText().toUpperCase().equals(ActionName.LOW.name())).findFirst().get();
		SWTBotToolbarButton filterInfoBtn = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).getToolbarButtons().stream().filter(btn -> btn.getToolTipText().toUpperCase().equals(ActionName.INFO.name())).findFirst().get();
		
		// Click to remove all filters
		filterHighBtn.click();
		filterMediumBtn.click();
		filterLowBtn.click();
		filterInfoBtn.click();
		
		// Asserts that no issues are visible in the tree once we are grouping by Severity and no severity is selected
		assertEquals(ASSERT_TREE_WITH_NO_ISSUES, _bot.tree().cell(0, 0), Environment.SCAN_ID + " (0 Issues)");
		
		// Click to include High severity
		clickSeverityFilter(ActionName.HIGH.name());
		currentActiveFilters.add(Severity.HIGH.name());
						
		sleep(1000);		
		
		String firstNodeName = _bot.tree().cell(0, 0);
		String secondNodeName = _bot.tree().getTreeItem(firstNodeName).expand().getNode(0).getText();
		String thirdNodeName = _bot.tree().getTreeItem(firstNodeName).expand().getNode(0).expand().getNode(0).getText();
		
		// Expand nodes until the first vulnerability
		String groupByQueryNameParent = _bot.tree().expandNode(firstNodeName).expandNode(secondNodeName).expandNode(thirdNodeName).getNode(0).getText();
		String groupByQueryNameChild = _bot.tree().expandNode(firstNodeName).expandNode(secondNodeName).expandNode(thirdNodeName).getNode(0).expand().getNode(0).getText();
		
		// Select the first vulnerability
		_bot.tree().expandNode(firstNodeName).expandNode(secondNodeName).expandNode(thirdNodeName).getNode(0).expand().getNode(0).select();
		
		// Asserts that the vulnerability has the same name as the parent node which means it is grouped by query name
		assertTrue(ASSERT_GROUP_BY_QUERY_NAME, groupByQueryNameParent.split("\\(")[0].trim().equals(groupByQueryNameChild));
		
		// Remove either group by severity and query name
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).viewMenu().menu(ToolBarActions.MENU_GROUP_BY).menu(ToolBarActions.GROUP_BY_QUERY_NAME).click();
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).viewMenu().menu(ToolBarActions.MENU_GROUP_BY).menu(ToolBarActions.GROUP_BY_SEVERITY).click();
		
		sleep(1000);
		
		firstNodeName = _bot.tree().cell(0, 0);
		secondNodeName = _bot.tree().getTreeItem(firstNodeName).expand().getNode(0).getText();
		_bot.tree().expandNode(firstNodeName).expandNode(secondNodeName);
				
		// Get's the first engine child
		String firstEngineChild = _bot.tree().expandNode(firstNodeName).expandNode(secondNodeName).getNode(0).getText();
		
		// Checks if it starts by HIGH, MEDIUM, LOW or INFO
		boolean engineChildDontStartWithHIGH = !firstEngineChild.startsWith(ActionName.HIGH.name());
		boolean engineChildDontStartWithMEDIUM = !firstEngineChild.startsWith(ActionName.MEDIUM.name());
		boolean engineChildDontStartWithLOW = !firstEngineChild.startsWith(ActionName.LOW.name());
		boolean engineChildDontStartWithINFO = !firstEngineChild.startsWith(ActionName.INFO.name());
		
		// Asserts group by options are not enabled
		assertTrue(ASSERT_NO_CHINDREN, _bot.tree().expandNode(firstNodeName).expandNode(secondNodeName).getNode(0).getNodes().isEmpty());
		assertTrue(ASSERT_GROUP_BY_SEVERITY_NOT_SELECTED, engineChildDontStartWithHIGH && engineChildDontStartWithMEDIUM && engineChildDontStartWithLOW && engineChildDontStartWithINFO);
		
		// Close Checkmarx AST Scan view
		_bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).close();
	}
	
	@Test(expected = WidgetNotFoundException.class)
	public void testInitialPanelWhenMissingCredentials() throws TimeoutException {
		// Add Checkmarx plugin to the eclipse view
		addCheckmarxPlugin(false);

		// Assert that active view is the Checkmarx AST Scan
		assertTrue("Active view must be the Checkmarx AST Scan", _bot.activeView().getTitle().equals(VIEW_CHECKMARX_AST_SCAN));
		
		assertTrue(ASSERT_CREDENTIALS_PANEL, _bot.button(PluginConstants.BTN_OPEN_SETTINGS) != null);
		
		_bot.button(PluginConstants.BTN_OPEN_SETTINGS).click();
		
		testSuccessfulConnection(true);
		
		// Button Open Settings must not be present at this moment so we are expecting WidgetNotFoundException in this test
		_bot.button(PluginConstants.BTN_OPEN_SETTINGS);
	}
	
	/**
	 * Set up checkmarx plugin
	 * 
	 * 		-> Set credentials
	 * 		-> Test connection
	 * 		-> Add checkmarx plugin
	 * 
	 * @throws TimeoutException
	 */
	private void setUpCheckmarxPlugin(boolean ignoreWrongScanValidation) throws TimeoutException{
		// Test Connection
		testSuccessfulConnection(false);

		// Add Checkmarx AST Plugin
		addCheckmarxPlugin(true);
		
		preventWidgetWasNullInCIEnvironment();
		
		if(!ignoreWrongScanValidation) {
			// Test incorrect Scan ID format
			_bot.comboBox(2).setText("invalid-scan-id");
			_bot.comboBox(2).pressShortcut(Keystrokes.LF);

			sleep(1000);

			assertEquals("The tree must contain one row with an error message", _bot.tree().rowCount(), 1);
			assertEquals("An incorrect scanId format message must be displayed", PluginConstants.TREE_INVALID_SCAN_ID_FORMAT, _bot.tree().cell(0, 0));
		}
		
		// type a valid and existing Scan ID
		typeValidScanID();

		assertEquals("The tree must contain one row", _bot.tree().rowCount(), 1);		
		boolean retrievingOrRetrievedResults = _bot.tree().cell(0, 0).contains(Environment.SCAN_ID);
		assertTrue("The plugin should have or should be retrieving results", retrievingOrRetrievedResults);

		waitWhileTreeNodeEqualsTo(String.format(PluginConstants.RETRIEVING_RESULTS_FOR_SCAN, Environment.SCAN_ID));
		
		assertTrue("The plugin should retrieve results", _bot.tree().cell(0, 0).startsWith(Environment.SCAN_ID));
	}
	
	/**
	 * Click on a severity filter
	 * 
	 * @param actionName
	 */
	private void clickSeverityFilter(String actionName) {
		SWTBotToolbarButton filterLowBtn = _bot.viewByTitle(VIEW_CHECKMARX_AST_SCAN).getToolbarButtons().stream().filter(btn -> btn.getToolTipText().toUpperCase().equals(actionName)).findFirst().get();
		filterLowBtn.click();
	}
	
	/**
	 * Expands the tree until the first engine and picks the list of available severities
	 * 
	 * @return
	 */
	private List<String> expandTreeUntilFirstEngineAndGetCurrentSeverities() {
		String firstNodeName = _bot.tree().cell(0, 0);
		String secondNodeName = _bot.tree().getTreeItem(firstNodeName).expand().getNode(0).getText();
		
		_bot.tree().expandNode(firstNodeName).expandNode(secondNodeName);
		
		return _bot.tree().getTreeItem(_bot.tree().cell(0, 0)).expand().getNode(0).getNodes().stream().map(node -> node.split("\\(")[0].trim()).collect(Collectors.toList());
	}

	/**
	 * Test successful connection
	 * 
	 * @throws TimeoutException
	 */
	private void testSuccessfulConnection(boolean openFromInitialPanel) throws TimeoutException {
		preventWidgetWasNullInCIEnvironment();
		
		if(_cxSettingsDefined) return;
		
		if(!openFromInitialPanel) {
			_bot.menu(TAB_WINDOW).menu(ITEM_PREFERENCES).click();
			_bot.shell(ITEM_PREFERENCES).activate();
			_bot.tree().select(ITEM_CHECKMARX_AST);
		}

		_bot.sleep(1000);

		_bot.shell(ITEM_PREFERENCES).setFocus(); // Need to set focus to avoid failing in CI environment
		
		_bot.textWithLabel(PluginConstants.PREFERENCES_SERVER_URL).setText(Environment.BASE_URL);
		_bot.textWithLabel(PluginConstants.PREFERENCES_TENANT).setText(Environment.TENANT);
		_bot.textWithLabel(PluginConstants.PREFERENCES_API_KEY).setText(Environment.API_KEY);

		_bot.button(BTN_APPLY).click();
		_bot.button(BTN_TEST_CONNECTION).click();
		
		//Do waitUntil Method to get text from text(6)
		waitForConnectionResponse();
		
		assertEquals(INFO_SUCCESSFUL_CONNECTION, _bot.text(6).getText());
			
		_bot.shell(ITEM_PREFERENCES).setFocus(); // Need to set focus to avoid failing in CI environment
		_bot.button(BTN_APPLY_AND_CLOSE).click();

		_cxSettingsDefined = true;
	}

	/**
	 * Add Checkmarx plugin in the show view perspective
	 * 
	 * @throws TimeoutException 
	 */
	private void addCheckmarxPlugin(boolean waitUntilPluginEnable) throws TimeoutException {
		preventWidgetWasNullInCIEnvironment();
		
		_bot.menu(TAB_WINDOW).menu(ITEM_SHOW_VIEW).menu(ITEM_OTHER).click();
		_bot.shell(ITEM_SHOW_VIEW).activate();
		_bot.tree().expandNode(ITEM_CHECKMARX).select(ITEM_CHECKMARX_AST_SCAN);
		_bot.button(BTN_OPEN).click();
		
		if(waitUntilPluginEnable) {
			waitUntilBranchComboIsEnabled();	
		}
		
	}

	/**
	 * Type a valid Scan ID to get results
	 * 
	 * @throws TimeoutException 
	 */
	private void typeValidScanID() throws TimeoutException {
		preventWidgetWasNullInCIEnvironment();
		
		_bot.comboBox(2).setText(Environment.SCAN_ID);
		_bot.comboBox(2).pressShortcut(Keystrokes.LF);
		
		waitUntilBranchComboIsEnabled();
	}

	/**
	 * Clear all Checkmarx credentials
	 */
	private void clearCheckmarxCredentials() {
		if (!_cxSettingsDefined) {
			return;
		}
		
		preventWidgetWasNullInCIEnvironment();

		_bot.menu(TAB_WINDOW).menu(ITEM_PREFERENCES).click();
		_bot.shell(ITEM_PREFERENCES).activate();
		_bot.tree().select(ITEM_CHECKMARX_AST);

		_bot.textWithLabel(PluginConstants.PREFERENCES_SERVER_URL).setText(PluginConstants.EMPTY_STRING);
		_bot.textWithLabel(PluginConstants.PREFERENCES_TENANT).setText(PluginConstants.EMPTY_STRING);
		_bot.textWithLabel(PluginConstants.PREFERENCES_API_KEY).setText(PluginConstants.EMPTY_STRING);

		_bot.button(BTN_APPLY).click();
		_bot.button(BTN_APPLY_AND_CLOSE).click();

		_cxSettingsDefined = false;
	}
	
	/**
	 * Wait while tree node equals to a a specific message. Fails after 10 retries
	 * 
	 * @param nodeText
	 * @throws TimeoutException
	 */
	private static void waitWhileTreeNodeEqualsTo(String nodeText) throws TimeoutException {
		int retryIdx = 0;

		while (_bot.tree().getAllItems()[0].getText().equals(nodeText)) {

			if (retryIdx == 10) {
				break;
			}

			_bot.sleep(1000);

			retryIdx++;
		}

		if (retryIdx == 10) {
			throw new TimeoutException("Timeout after 5000ms. Scan results should be retrieved");
		}
	}
	
	/**
	 * Wait until branch combobox is enabled
	 * 
	 * @throws TimeoutException
	 */
	private static void waitUntilBranchComboIsEnabled() throws TimeoutException {
		preventWidgetWasNullInCIEnvironment();
		
		boolean emptyScanId = _bot.comboBox(2).getText().isEmpty() || _bot.comboBox(2).getText().equals(PluginConstants.COMBOBOX_SCAND_ID_PLACEHOLDER);
		
		if(emptyScanId) {
			return;
		}
		
		int retryIdx = 0;

		while (!_bot.comboBox(1).isEnabled()) {

			if (retryIdx == 10) {
				break;
			}

			_bot.sleep(1000);

			retryIdx++;
		}

		if (retryIdx == 10) {
			throw new TimeoutException("Timeout after 5000ms. Branches' combobox must be enabled");
		}
	}
	
	/**
	 * Wait while tree node equals to a a specific message. Fails after 10 retries
	 * 
	 * @param nodeText
	 * @throws TimeoutException
	 */
	private static void waitForConnectionResponse() throws TimeoutException {
		int retryIdx = 0;

		while (!_bot.text(6).getText().equals(INFO_SUCCESSFUL_CONNECTION)) {
			if (retryIdx == 10) {
				break;
			}

			_bot.sleep(1000);
			retryIdx++;
		}

		if (retryIdx == 10) {
			throw new TimeoutException("Connection validation timeout after 10000ms.");
		}
	}
}
