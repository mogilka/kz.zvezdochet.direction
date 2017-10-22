package kz.zvezdochet.direction.part;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.Tab;
import kz.zvezdochet.core.ui.comparator.TableSortListenerFactory;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.listener.ListSelectionListener;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelPart;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.bean.Member;
import kz.zvezdochet.direction.bean.Participant;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.provider.EventProposalProvider;
import kz.zvezdochet.provider.EventProposalProvider.EventContentProposal;

/**
 * Представление синастрии
 * @author Nataly Didenko
 *
 */
public class CollationPart extends ModelPart implements ICalculable {
	@Inject
	public CollationPart() {}

	private Text txEvent;
	private Text txDescription;
	private TableViewer tvParticipants;
	private Table tbParticipants;
	private TableViewer tvMembers;
	private Table tbMembers;
	private CTabFolder folder;
	private Group grAspects;
	private Group grDirections;
	private Group grHouses;
	private CTabFolder subfolder;
	private Group grSubaspects;
	private Group grSubdirections;
	private Group grSubhouses;

	@PostConstruct @Override
	public View create(Composite parent) {
		// событие
		Label lb = new Label(parent, SWT.NONE);
		lb.setText("Событие");
		txEvent = new Text(parent, SWT.BORDER);
		new InfoDecoration(txEvent, SWT.TOP | SWT.LEFT);
		txEvent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txEvent.setFocus();

		lb = new Label(parent, SWT.NONE);
		lb.setText("Описание");
		txDescription = new Text(parent, SWT.BORDER);
		txDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		EventProposalProvider proposalProvider = new EventProposalProvider(new Object[] {0});
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txEvent, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null) {
					Collation collation = new Collation();
					collation.setEvent(event);
					setModel(collation, true);
				}
			}
		});

	    // участники
	    Group grParticipants = new Group(parent, SWT.NONE);
		grParticipants.setText("Участники");
		grParticipants.setLayout(new GridLayout());
		grParticipants.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Text txParticipant = new Text(grParticipants, SWT.BORDER);
		new InfoDecoration(txParticipant, SWT.TOP | SWT.LEFT);
		txParticipant.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		proposalProvider = new EventProposalProvider(new Object[] {1,2});
	    adapter = new ContentProposalAdapter(
	        txParticipant, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null)
					addParticipant(event);
			}
		});

	    tvParticipants = new TableViewer(grParticipants, SWT.BORDER | SWT.FULL_SELECTION);
		tbParticipants = tvParticipants.getTable();
		tbParticipants.setHeaderVisible(true);
		tbParticipants.setLinesVisible(true);

		String[] columns = new String[] {
			"Имя",
			"Дата",
			"Победитель" };
		int i = -1;
		for (String column : columns) {
			TableViewerColumn tableColumn = new TableViewerColumn(tvParticipants, SWT.NONE);
			tableColumn.getColumn().setText(column);		
			tableColumn.getColumn().addListener(SWT.Selection, TableSortListenerFactory.getListener(
				TableSortListenerFactory.STRING_COMPARATOR));
			if (++i > 1)
				tableColumn.setEditingSupport(new ParticipantEditingSupport(tvParticipants));
		}
		tvParticipants.setContentProvider(new ArrayContentProvider());
		tvParticipants.setLabelProvider(getParticipantProvider());

		ListSelectionListener listener = new ListSelectionListener();
		tvParticipants.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					IStructuredSelection sel = (IStructuredSelection)event.getSelection();
					if (sel.getFirstElement() != null) {
						Participant participant = (Participant)sel.getFirstElement();
						initMemberTable(participant.getMembers());
					}
				}				
			}
		});
		tvParticipants.addDoubleClickListener(listener);

		//вкладки участников
		Group grTabs = new Group(parent, SWT.NONE);
		folder = new CTabFolder(grTabs, SWT.BORDER);
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
		GridLayoutFactory.swtDefaults().applyTo(grTabs);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grTabs);

		super.create(parent);
		return null;
	}

	/**
	 * Добавление фигуранта события
	 * @param event фигурант
	 */
	private void addMember(Event event) {
		try {
			IStructuredSelection sel = (IStructuredSelection)tvParticipants.getSelection();
			if (null == sel.getFirstElement()) {
				DialogUtil.alertError("Задайте сообщество, в которое добавляется участник");
				return;
			} else {
				Event participant = (Event)sel.getFirstElement();
				List<Event> members = participant.getMembers();
				if (null == members)
					members = new ArrayList<Event>();
				if (members.contains(event))
					return;
				members.add(event);
				participant.setMembers(members);
				tvMembers.add(event);
				tvMembers.setSelection(new StructuredSelection(event));
				((Collation)model).setNeedSaveRel(true);
			}				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Добавление участника события
	 * @param event участник
	 */
	private void addParticipant(Event event) {
		try {
			Collation collation = (Collation)getModel(0, false);
			if (null == collation)
				DialogUtil.alertError("Задайте событие, в которое добавляется участник");
			else {
				if (null == event)
					DialogUtil.alertError("Задайте участника события");
				else {
					List<Participant> participants = collation.getParticipants();
					if (null == participants)
						participants = new ArrayList<Participant>();
					else {
						for (Participant participant : participants)
							if (participant.getEvent().getId() == event.getId())
								return;
					}
					Participant participant = new Participant(event, collation);
					participants.add(participant);
					collation.setParticipants(participants);
					tvParticipants.add(event);
					tvParticipants.setSelection(new StructuredSelection(event));
					collation.setNeedSaveRel(true);
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean check(int mode) throws Exception {
		if (Handler.MODE_SAVE == mode) {
			if (null == model) {
				DialogUtil.alertError("Выберите событие");
				return false;
			}
		}
		return true;
	}

	private IBaseLabelProvider getParticipantProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Participant participant = (Participant)element;
				Event event = participant.getEvent();
				switch (columnIndex) {
					case 0: return event.getName();
					case 1: return DateUtil.formatDateTime(event.getBirth());
				}
				return null;
			}
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				Participant participant = (Participant)element;
				switch (columnIndex) {
					case 2: return participant.isWin() ? CHECKED : UNCHECKED;
				}
				return null;
			}
		};
	}

	private final Image CHECKED = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.core", "icons/checked.gif").createImage();
    private final Image UNCHECKED = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.core", "icons/unchecked.gif").createImage();

    /**
     * Прорисовщик фигурантов
     * @return значение ячейки
     * @see http://www.vogella.com/tutorials/EclipseJFaceTableAdvanced/article.html
     * TODO When you need to change an image call TreeViewer.update or TreeViewer.refresh 
     * if the children of the object also need refreshing. This will call the label provider again
     */
	private IBaseLabelProvider getMemberProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Member member = (Member)element;
				Event event = member.getEvent();
				switch (columnIndex) {
					case 0: return event.getName();
					case 1: return DateUtil.formatDateTime(event.getBirth());
				}
				return null;
			}
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				Member member = (Member)element;
				switch (columnIndex) {
					case 2: return member.isHit() ? CHECKED : UNCHECKED;
					case 3: return member.isPass() ? CHECKED : UNCHECKED;
					case 4: return member.isMiss() ? CHECKED : UNCHECKED;
					case 5: return member.isSave() ? CHECKED : UNCHECKED;
					case 6: return member.isFoul() ? CHECKED : UNCHECKED;
					case 7: return member.isSubstitute() ? CHECKED : UNCHECKED;
				}
				return null;
			}
		};
	}

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tbParticipants);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tbMembers);
	}
	
	@Override
	public void onCalc(Object obj) {
//		System.out.println(obj);
		try {
//			txText.setText(obj.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void syncView() {
		if (null == model) return;
		Collation collation = (Collation)model;
		if (null == collation.getEvent())
			txEvent.setText("");
		else
			txEvent.setText(collation.getEvent().getName());
		txDescription.setText(collation.getDescription());
		initParticipantTable(collation.getParticipants());
		initMemberTable(null);
	}

	@Override
	public void syncModel(int mode) throws Exception {
		// TODO Auto-generated method stub
	}

	/**
	 * Инициализация таблицы участников
	 */
	private void initParticipantTable(List<Participant> data) {
		try {
			showBusy(true);
			if (null == data)
				tbParticipants.removeAll();
			else
				tvParticipants.setInput(data);
			for (int i = 0; i < tbParticipants.getColumnCount(); i++)
				tbParticipants.getColumn(i).pack();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			showBusy(false);
		}
	}

	/**
	 * Инициализация таблицы фигурантов
	 */
	private void initMemberTable(List<Member> data) {
		try {
			showBusy(true);
			if (null == data)
				tbMembers.removeAll();
			else
				tvMembers.setInput(data);
			for (int i = 0; i < tbMembers.getColumnCount(); i++)
				tbMembers.getColumn(i).pack();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			showBusy(false);
		}
	}

	/**
	 * Быстрое редактирование таблицы фигурантов
	 * @author nataly
	 *
	 */
	public class MemberEditingSupport extends EditingSupport {
	    private final TableViewer viewer;
	    private int type;

	    public MemberEditingSupport(TableViewer viewer, int type) {
	        super(viewer);
	        this.viewer = viewer;
	        this.type = type;
	    }

	    @Override
	    protected CellEditor getCellEditor(Object element) {
	        return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
	    }

	    @Override
	    protected boolean canEdit(Object element) {
	        return true;
	    }

	    @Override
	    protected Object getValue(Object element) {
	        Member member = (Member)element;
	        switch (type) {
				case 2: return member.isHit();
				case 3: return member.isPass();
				case 4: return member.isMiss();
				case 5: return member.isSave();
				case 6: return member.isFoul();
				case 7: return member.isSubstitute();
	        }
	        return null;
	    }
	    @Override
	    protected void setValue(Object element, Object value) {
	        Member member = (Member)element;
	        boolean val = (Boolean)value;
	        switch (type) {
				case 2: member.setHit(val); break;
				case 3: member.setPass(val); break;
				case 4: member.setMiss(val); break;
				case 5: member.setSave(val); break;
				case 6: member.setFoul(val); break;
				case 7: member.setSubstitute(val);
	        }
	        viewer.update(element, null);
	    }
	}

	/**
	 * Быстрое редактирование таблицы участников
	 * @author nataly
	 *
	 */
	public class ParticipantEditingSupport extends EditingSupport {
	    private final TableViewer viewer;

	    public ParticipantEditingSupport(TableViewer viewer) {
	        super(viewer);
	        this.viewer = viewer;
	    }

	    @Override
	    protected CellEditor getCellEditor(Object element) {
	        return new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY);
	    }

	    @Override
	    protected boolean canEdit(Object element) {
	        return true;
	    }

	    @Override
	    protected Object getValue(Object element) {
	        Participant participant = (Participant)element;
			return participant.isWin();
	    }
	    @Override
	    protected void setValue(Object element, Object value) {
	        Participant participant = (Participant)element;
			participant.setWin((Boolean)value);
	        viewer.update(element, null);
	    }
	}

	/**
	 * Инициализация вкладок космограммы
	 * @return массив вкладок
	 */
	private Tab[] initTabs() {
		Tab[] tabs = new Tab[4];
		//фигуранты
		Tab tab = new Tab();
		tab.name = "Фигуранты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/contacts.gif").createImage();

		Group grMembers = new Group(folder, SWT.NONE);
		grMembers.setLayout(new GridLayout());
		grMembers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Text txMember = new Text(grMembers, SWT.BORDER);
		new InfoDecoration(txMember, SWT.TOP | SWT.LEFT);
		txMember.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		EventProposalProvider proposalProvider = new EventProposalProvider(new Object[] {1});
		ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txMember, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null)
					addMember(event);
			}
		});

	    tvMembers = new TableViewer(grMembers, SWT.BORDER | SWT.FULL_SELECTION);
		tbMembers = tvMembers.getTable();
		tbMembers.setHeaderVisible(true);
		tbMembers.setLinesVisible(true);
		String[] columns = new String[] {
			"Имя",
			"Дата",
			"Гол",
			"Пас",
			"Промах",
			"Сэйв",
			"Фол",
			"Замена" };
		int i = -1;
		for (String column : columns) {
			TableViewerColumn tableColumn = new TableViewerColumn(tvMembers, SWT.NONE);
			tableColumn.getColumn().setText(column);		
			tableColumn.getColumn().addListener(SWT.Selection, TableSortListenerFactory.getListener(
				TableSortListenerFactory.STRING_COMPARATOR));
			if (++i > 1)
				tableColumn.setEditingSupport(new MemberEditingSupport(tvMembers, i));
		}
		tvMembers.setContentProvider(new ArrayContentProvider());
		tvMembers.setLabelProvider(getMemberProvider());

		ListSelectionListener listener = new ListSelectionListener();
		tvMembers.addSelectionChangedListener(listener);
		tvMembers.addDoubleClickListener(listener);

		//вкладки фигуранта
		Group grTabs = new Group(grMembers, SWT.NONE);
		subfolder = new CTabFolder(grTabs, SWT.BORDER);
		subfolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		subfolder.setSimple(false);
		subfolder.setUnselectedCloseVisible(false);
		Tab[] subtabs = initSubtabs();
		for (Tab subtab : subtabs) {
			CTabItem item = new CTabItem(subfolder, SWT.CLOSE);
			item.setText(subtab.name);
			item.setImage(subtab.image);
			item.setControl(subtab.control);
		}
		subfolder.pack();
		GridLayoutFactory.swtDefaults().applyTo(grTabs);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grTabs);

		tab.control = grMembers;
		tabs[0] = tab;

		//аспекты
		tab = new Tab();
		tab.name = "Аспекты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect.gif").createImage();
		grAspects = new Group(folder, SWT.NONE);
		Object[] titles = {
			"Планета",
			"Аспект",
			"Планета"
		};
		Table table = new Table(grAspects, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grAspects.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (Object title : titles) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grAspects;
		GridLayoutFactory.swtDefaults().applyTo(grAspects);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grAspects);
		tabs[1] = tab;
		
		//дирекции
		tab = new Tab();
		tab.name = "Дирекции";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.direction", "icons/direction.gif").createImage();
		grDirections = new Group(folder, SWT.NONE);
		String[] titles2 = {
			"Планета",
			"Аспект",
			"Дом"};
		table = new Table(grDirections, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grDirections.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (String title : titles2) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText(title);
		}
		tab.control = grDirections;
		GridLayoutFactory.swtDefaults().applyTo(grDirections);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grDirections);
		tabs[2] = tab;
		
		//дома
		tab = new Tab();
		tab.name = "Дома";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/home.gif").createImage();
		grHouses = new Group(folder, SWT.NONE);
		Object[] titles3 = {
			"Планета",
			"Дом"
		};
		table = new Table(grHouses, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grHouses.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (Object title : titles3) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grHouses;
		tabs[3] = tab;
		return tabs;
	}

	/**
	 * Инициализация вкладок фигуранта
	 * @return массив вкладок
	 */
	private Tab[] initSubtabs() {
		Tab[] tabs = new Tab[3];

		//аспекты
		Tab tab = new Tab();
		tab.name = "Аспекты";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/aspect.gif").createImage();
		grSubaspects = new Group(subfolder, SWT.NONE);
		Object[] titles = {
			"Планета",
			"Аспект",
			"Планета"
		};
		Table table = new Table(grSubaspects, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grSubaspects.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (Object title : titles) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grSubaspects;
		GridLayoutFactory.swtDefaults().applyTo(grSubaspects);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grSubaspects);
		tabs[0] = tab;
		
		//дирекции
		tab = new Tab();
		tab.name = "Дирекции";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet.direction", "icons/direction.gif").createImage();
		grSubdirections = new Group(subfolder, SWT.NONE);
		String[] titles2 = {
			"Планета",
			"Аспект",
			"Дом"};
		table = new Table(grSubdirections, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grSubdirections.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (String title : titles2) {
			TableColumn column = new TableColumn (table, SWT.NONE);
			column.setText(title);
		}
		tab.control = grSubdirections;
		GridLayoutFactory.swtDefaults().applyTo(grSubdirections);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(grSubdirections);
		tabs[1] = tab;
		
		//дома
		tab = new Tab();
		tab.name = "Дома";
		tab.image = AbstractUIPlugin.imageDescriptorFromPlugin("kz.zvezdochet", "icons/home.gif").createImage();
		grSubhouses = new Group(subfolder, SWT.NONE);
		Object[] titles3 = {
			"Планета",
			"Дом"
		};
		table = new Table(grSubhouses, SWT.BORDER | SWT.V_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setSize(grSubhouses.getSize());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
		for (Object title : titles3) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(title.toString());
		}	
		tab.control = grSubhouses;
		tabs[2] = tab;
		return tabs;
	}
}
