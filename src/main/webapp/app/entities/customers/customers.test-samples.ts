import { ICustomers, NewCustomers } from './customers.model';

export const sampleWithRequiredData: ICustomers = {
  id: 77988,
  username: 'Money',
  password: 'Chair transmit HDD',
  phoneNumber: 'lime',
  balance: 35716,
};

export const sampleWithPartialData: ICustomers = {
  id: 69775,
  username: 'digital Clothing repurpose',
  password: 'Rubber application Quality-focused',
  phoneNumber: 'Bedfordshire grey Manors',
  balance: 28332,
};

export const sampleWithFullData: ICustomers = {
  id: 15906,
  username: 'Rubber Investor',
  password: 'plum',
  phoneNumber: 'ivory',
  balance: 56934,
};

export const sampleWithNewData: NewCustomers = {
  username: 'visualize',
  password: 'integrated',
  phoneNumber: 'Accounts Re-contextualized',
  balance: 21952,
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
