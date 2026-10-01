import React from 'react';

export const CATEGORY_CONTACT_EMAIL = 'jms663100@kyungnam.ac.kr';

export const getSubcategories = (categories, majorCategoryId) => (
    categories.find((category) => String(category.id) === String(majorCategoryId))?.subcategories || []
);

export const AssetCategoryContact = () => (
    <p className="asset-category-contact">
        적합한 분류가 없나요? 임의로 선택하지 말고{' '}
        <a href={`mailto:${CATEGORY_CONTACT_EMAIL}`}>{CATEGORY_CONTACT_EMAIL}</a>로 문의해 주세요.
    </p>
);

const AssetCategoryFields = ({ categories, majorCategoryId, categoryId, onChange, errors = {} }) => (
    <>
        <div className="form-group">
            <label htmlFor="asset-major-category">대분류 <span className="required">*</span></label>
            <select
                id="asset-major-category"
                name="majorCategoryId"
                className="form-select"
                value={majorCategoryId || ''}
                onChange={onChange}
                required
            >
                <option value="">대분류 선택</option>
                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
            {errors.majorCategoryId && <span className="error-message">{errors.majorCategoryId}</span>}
        </div>
        <div className="form-group">
            <label htmlFor="asset-subcategory">중분류 <span className="required">*</span></label>
            <select
                id="asset-subcategory"
                name="categoryId"
                className="form-select"
                value={categoryId || ''}
                onChange={onChange}
                disabled={!majorCategoryId}
                required
            >
                <option value="">중분류 선택</option>
                {getSubcategories(categories, majorCategoryId).map((category) => (
                    <option key={category.id} value={category.id}>{category.name}</option>
                ))}
            </select>
            {errors.categoryId && <span className="error-message">{errors.categoryId}</span>}
        </div>
        <AssetCategoryContact />
    </>
);

export default AssetCategoryFields;
