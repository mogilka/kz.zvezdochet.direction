package kz.zvezdochet.direction.part;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.Tab;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.service.CollationService;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Поиск событий с участниками
 * @author Nataly Didenko
 */
public class SearchPart extends ModelListView {
	private CTabFolder folder;
	
	@Inject
	public SearchPart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
//		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
//			@Override
//			public void doubleClick(DoubleClickEvent event) {
//				new EventHandler().execute(SearchPart.this);				
//			}
//		});
		return null;
	}
	
	@Override
	public void initFilter() {
		grFilter = new Group(container, SWT.NONE);
		grFilter.setText("Поиск");
		grFilter.setLayout(new GridLayout());
		folder = new CTabFolder(grFilter, SWT.BORDER);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.setSimple(false);
		folder.setUnselectedCloseVisible(false);
		Tab[] tabs = initTabs();
		for (Tab tab : tabs) {
			CTabItem item = new CTabItem(folder, SWT.CLOSE);
			item.setText(tab.name);
			item.setImage(tab.image);
			item.setControl(tab.control);
		}
		folder.setSelection(0);
	}

	/**
	 * Инициализация вкладок поиска
	 * @return массив вкладок
	 */
	private Tab[] initTabs() {
		Tab[] tabs = new Tab[3];
		Tab tab = new Tab();
		tab.name = "по имени";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.core", "icons/correction_linked_rename.gif").createImage();
		final Text txSearch = new Text(folder, SWT.BORDER);
		txSearch.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		txSearch.setFocus();
		txSearch.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String text = txSearch.getText();
				if (text.length() > 1)
					try {
						setData(new CollationService().findByName(text));
					} catch (DataAccessException e) {
						e.printStackTrace();
					}			
			}
		});
		tab.control = txSearch;
		tabs[0] = tab;
		
		////////////////////////////////////////////////////////////////

		tab = new Tab();
		tab.name = "по дате";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/calendar_view_day.png").createImage();
		Group group = new Group(folder, SWT.NONE);

		Label lb = new Label(group, SWT.NONE);
		lb.setText("Начало");
		final CDateTime dt = new CDateTime(group, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.TIME_MEDIUM);
		dt.setNullText(""); //$NON-NLS-1$

		lb = new Label(group, SWT.NONE);
		lb.setText("Конец");
		final CDateTime dt2 = new CDateTime(group, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.TIME_MEDIUM);
		dt2.setNullText(""); //$NON-NLS-1$

		Button bt = new Button(group, SWT.NONE);
		bt.setText("Искать");
		bt.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (null == dt.getSelection() || null == dt2.getSelection())
					return;
				try {
					setData(new CollationService().findByDateRange(dt.getSelection(), dt2.getSelection()));
				} catch (DataAccessException e1) {
					e1.printStackTrace();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		tab.control = group;
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		tabs[1] = tab;
		
		/////////////////////////////////////////////////////////////////////
		
		tab = new Tab();
		tab.name = "по номеру";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/keycolumn.gif").createImage();
		final Text txNumber = new Text(folder, SWT.BORDER);
		txNumber.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		txNumber.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String text = txNumber.getText();
				if (text.length() > 1)
					try {
						Model model = new CollationService().find(Long.valueOf(text));
						if (model != null)
							setData(new Model[] {model});
					} catch (DataAccessException e) {
						e.printStackTrace();
					}
			}
		});
		tab.control = txNumber;
		tabs[2] = tab;

		return tabs;
	}

	@Override
	protected String[] initTableColumns() {
		String[] columns = {
			"№",
			"Имя",
			"Дата",
			"Описание",
			"Дата изменения" };
		return columns;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Collation model = (Collation)element;
				kz.zvezdochet.bean.Event event = model.getEvent();
				if (model != null)
					switch (columnIndex) {
						case 1: return model.getId().toString();
						case 2: return event.getName();
						case 3: return DateUtil.formatDateTime(event.getBirth());
						case 4: return model.getDescription();
						case 5: return DateUtil.formatDateTime(model.getCreated_at());
					}
				return null;
			}
		};
	}

	@Override
	public boolean check(int mode) throws Exception {
		return false;
	}

	@Override
	public Model createModel() {
		return new Collation();
	}
}
