package kz.zvezdochet.direction.part;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.Tab;
import kz.zvezdochet.core.ui.decoration.RequiredDecoration;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.part.CosmogramComposite;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.util.Configuration;

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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Таблица транзитов на указанную дату
 * @author Nataly Didenko
 */
public class DatePart extends ModelListView implements ICalculable {
	private CDateTime dtBirth;
	private Event trevent;

	@Inject
	public DatePart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		Group grCosmogram = new Group(parent, SWT.NONE);
		grCosmogram.setText("Космограмма");
		cmpCosmogram = new CosmogramComposite(grCosmogram, SWT.NONE);

		folder = new CTabFolder(grCosmogram, SWT.BORDER);
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
		folder.pack();
		GridLayoutFactory.swtDefaults().applyTo(grCosmogram);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grCosmogram);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
		return null;
	}
	
	@Override
	public void initFilter() {
		grFilter = new Group(container, SWT.NONE);
		grFilter.setText("Поиск");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Дата");
		dtBirth = new CDateTime(grFilter, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.TIME_MEDIUM);
		dtBirth.setNullText(""); //$NON-NLS-1$
		new RequiredDecoration(lb, SWT.TOP | SWT.RIGHT);

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).
			grab(false, false).applyTo(dtBirth);
	}

	@Override
	protected String[] initTableColumns() {
		String[] columns = {
			"Точка 1",
			"Аспект",
			"Точка 2",
			"Величина аспекта" };
		return columns;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				SkyPointAspect aspect = (SkyPointAspect)element;
				switch (columnIndex) {
					case 0: return aspect.getSkyPoint1().getName();
					case 1: return aspect.getAspect().getName();
					case 2: return aspect.getSkyPoint2().getName();
					case 3: return String.valueOf(aspect.getScore());
				}
				return null;
			}
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				SkyPointAspect aspect = (SkyPointAspect)element;
				switch (columnIndex) {
					case 0: return aspect.getSkyPoint1() instanceof Planet
							? ((Planet)aspect.getSkyPoint1()).getImage() : null;
					case 2: return aspect.getSkyPoint2() instanceof Planet
							? ((Planet)aspect.getSkyPoint2()).getImage() : null;
				}
				return null;
			}
			@Override
			public Color getForeground(Object element, int columnIndex) {
				if (1 == columnIndex) {
					SkyPointAspect aspect = (SkyPointAspect)element;
					if (aspect.getAspect() != null)
						return aspect.getAspect().getType().getColor();
				}
				return  Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			}
		};
	}

	private Event person;

	/**
	 * Поиск персоны, для которой делается прогноз
	 * @return персона
	 */
	public Event getPerson() {
		return person;
	}

	/**
	 * Инициализация персоны, для которой делается прогноз
	 * @param event персона
	 */
	public void setPerson(Event event) {
		this.person = event;
	}

	@Override
	protected void initControls() {
		dtBirth.setSelection(new Date());
	}

	@Override
	public boolean check(int mode) {
		if (null == dtBirth.getSelection()) {
			DialogUtil.alertError("Укажите дату");
			return false;
		} else if (null == person) {
			DialogUtil.alertError("Событие не задано");
			return false;
		} else if (null == person.getConfiguration()) {
			DialogUtil.alertError("Конфигурация события не задана");
			return false;
		}
		return true;
	}

	/**
	 * Возвращает выбранную дату
	 * @return дата транзита
	 */
	public Date getDate() {
		return dtBirth.getSelection();
	}

	/**
	 * Режим расчёта транзитов.
	 * 1 - по умолчанию отображаются планеты события в карте персоны.
	 * 0 - режим планет персоны в карте события
	 */
	private int MODE_CALC = 1;

	private CosmogramComposite cmpCosmogram;
	private CTabFolder folder;
	private Group grPlanets;
	private Group grHouses;
	private Group grAspectType;

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
	}

	/**
	 * Перерисовка космограммы
	 * @param event событие|персона
	 * @param event2 событие|персона
	 */
	private void refreshCard(Event event, Event event2) {
		List<String> params = new ArrayList<String>();
		Map<String, String[]> types = AspectType.getHierarchy();
		for (Control control : grAspectType.getChildren()) {
			Button button = (Button)control;
			if (button.getSelection())
				params.addAll(Arrays.asList(types.get(button.getData("type"))));
		}
		if (params.size() < 1) return;
		cmpCosmogram.paint(event.getConfiguration(), event2.getConfiguration(), params);
	}

	/**
	 * Инициализация вкладок космограммы
	 * @return массив вкладок
	 */
	private Tab[] initTabs() {
		Tab[] tabs = new Tab[4];
		//настройки расчёта
		Tab tab = new Tab();
		tab.name = "Настройки";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.runner", "icons/configure.gif").createImage();
		Group group = new Group(folder, SWT.NONE);
		group.setText("Общие");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tab.control = group;
		tabs[0] = tab;

		//планеты
		tab = new Tab();
		tab.name = "Планеты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/planet.gif").createImage();
		grPlanets = new Group(folder, SWT.NONE);
		Object[] titles = { "Планета", "Координата #1", "Координата #2"	};
		Table table = new Table(grPlanets, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grPlanets.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (Object title : titles) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grPlanets;
		GridLayoutFactory.swtDefaults().applyTo(grPlanets);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grPlanets);
		tabs[1] = tab;
		
		//дома
		tab = new Tab();
		tab.name = "Дома";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/home.gif").createImage();
		grHouses = new Group(folder, SWT.NONE);
		String[] titles2 = {"Дом", "Координата #1", "Координата #2"};
		table = new Table(grHouses, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grHouses.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (int i = 0; i < titles2.length; i++) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText(titles2[i]);
		}
		tab.control = grHouses;
		GridLayoutFactory.swtDefaults().applyTo(grHouses);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grHouses);
		tabs[2] = tab;
		
		//аспекты
		tab = new Tab();
		tab.name = "Аспекты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect.gif").createImage();
		grAspectType = new Group(folder, SWT.NONE);
		grAspectType.setLayout(new GridLayout());
		List<Model> types = new ArrayList<Model>();
		try {
			types = new AspectTypeService().getList();
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		for (Model model : types) {
			AspectType type = (AspectType)model;
			if (type.getImage() != null) {
				final Button bt = new Button(grAspectType, SWT.BORDER | SWT.CHECK);
				bt.setText(type.getName());
				bt.setImage(AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect/" + type.getImage()).createImage());
				bt.setSelection(true);
				bt.setData("type", type.getCode());
				bt.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						if (bt.getSelection())
							onCalc(MODE_CALC);
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {}
				});
			}
		}
		tab.control = grAspectType;
		tabs[3] = tab;
		
		return tabs;
	}

	/**
	 * Обновление вкладок
	 */
	private void refreshTabs(Event partner, Event partner2) {
		//планеты
		Control[] controls = grPlanets.getChildren();
		Table table = (Table)controls[0];
		table.removeAll();
		Configuration conf = partner.getConfiguration();
		Configuration conf2 = partner2.getConfiguration();
		if (conf != null) {
			folder.setSelection(1);
			int j = -1;
			for (Model base : conf.getPlanets()) {
				++j;
				Planet planet = (Planet)base;
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, planet.getName());
				item.setText(1, String.valueOf(planet.getCoord()));
				//планеты партнёра
				if (conf2 != null) {
					planet = (Planet)conf2.getPlanets().get(j);
					item.setText(2, String.valueOf(planet.getCoord()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		} else
			folder.setSelection(0);
			
		//дома
		controls = grHouses.getChildren();
		table = (Table)controls[0];
		table.removeAll();
		if (conf != null) {
			int j = -1;
			for (Model base : conf.getHouses()) {
				++j;
				House house = (House)base;
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, house.getName());		
				item.setText(1, String.valueOf(house.getCoord()));
				//дома партнёра
				if (conf2 != null) {
					house = (House)conf2.getHouses().get(j);
					item.setText(2, String.valueOf(house.getCoord()));
				}
			}
			for (int i = 0; i < table.getColumnCount(); i++)
				table.getColumn(i).pack();
		}
	}

	@Override
	public void onCalc(Object mode) {
		MODE_CALC = (int)mode;
		System.out.println("onCalc" + MODE_CALC);
		if (mode.equals(0)) {
			refreshCard(person, trevent);
			refreshTabs(person, trevent);
		} else {
			refreshCard(trevent, person);
			refreshTabs(trevent, person);
		}
	}

	/**
	 * Возвращает режим представления для расчёта
	 * @return 0 - планеты человека в домах события,
	 * 1 - планеты события в домах человека
	 */
	public int getModeCalc() {
		return MODE_CALC;
	}

	/**
	 * Инициализация транзитного события
	 * @param event событие
	 */
	public void setEvent(Event event) {
		trevent = event;
	}
}
