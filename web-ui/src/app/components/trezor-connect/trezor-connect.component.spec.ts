import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TrezorConnectComponent } from './trezor-connect.component';
import { ExplorerCurrencyPipe } from '../../pipes/explorer-currency.pipe';
import { AddressesService } from '../../services/addresses.service';
import { TransactionsService } from '../../services/transactions.service';
import { TrezorRepositoryService } from '../../services/trezor-repository.service';
import { TrezorAddress } from '../../trezor/trezor-helper';
import { TranslateModule } from '@ngx-translate/core';

import { Observable } from 'rxjs';

import { NO_ERRORS_SCHEMA, } from '@angular/core';

describe('TrezorConnectComponent', () => {
  let component: TrezorConnectComponent;
  let fixture: ComponentFixture<TrezorConnectComponent>;

  const addressesServiceSpy: jasmine.SpyObj<AddressesService> = jasmine
    .createSpyObj('AddressesService', ['getUtxos']);
  const transactionsServiceSpy: jasmine.SpyObj<TransactionsService> = jasmine
    .createSpyObj('TransactionsService', ['getRaw', 'push']);
  const trezorRepositoryServiceSpy: jasmine.SpyObj<TrezorRepositoryService> = jasmine
    .createSpyObj('TrezorRepositoryService', ['get', 'add']);

  beforeEach(async(() => {
    transactionsServiceSpy.getRaw.and.returnValue(Observable.create());
    trezorRepositoryServiceSpy.get.and.returnValue(Array<TrezorAddress>());

    TestBed.configureTestingModule({
      declarations: [
        TrezorConnectComponent,
        ExplorerCurrencyPipe
      ],
      imports: [
        TranslateModule.forRoot()
      ],
      providers: [
        { provide: AddressesService, useValue: addressesServiceSpy },
        { provide: TransactionsService, useValue: transactionsServiceSpy },
        { provide: TrezorRepositoryService, useValue: trezorRepositoryServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TrezorConnectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
